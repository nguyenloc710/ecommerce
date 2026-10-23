# Thiết kế hệ thống thương mại điện tử (thời trang, phụ kiện, trang sức)

> Phiên bản 3.1: **Microservice tinh gọn (5 service) + Keycloak trên Kubernetes chi phí thấp**.
> Backend: Java Spring Boot. Frontend: Next.js. Quy mô: nhà bán hàng nhỏ, có lộ trình mở rộng.

---

## 1. Mục tiêu và nguyên tắc

| Mục tiêu | Cách đáp ứng |
|---|---|
| Chi phí hạ tầng thấp (~1–2 triệu VND/tháng) | k3s tự dựng trên 3 VPS, dữ liệu chạy trong cluster, dùng dịch vụ miễn phí cho CDN, email, giám sát |
| Ít service, dễ vận hành | 5 service; mỗi service gồm nhiều module bên trong (Spring Modulith) |
| Đăng nhập chuẩn, không tự viết | **Keycloak**: OIDC, đăng nhập Google, MFA cho nhân viên, quên mật khẩu, chống brute force |
| Sẵn sàng tách nhỏ khi lớn | Ranh giới module rõ ràng, giao tiếp bằng event, schema DB tách theo module |
| Không mất đơn, không bán âm kho | Giữ hàng có TTL, idempotency, outbox (Event Publication Registry của Spring Modulith) |
| SEO tốt | Next.js SSR/ISR, ảnh qua CDN |

Nguyên tắc:
1. **Mỗi service một database riêng**, không join DB giữa các service.
2. **Bên trong service chia module** theo nghiệp vụ. Module không gọi thẳng repository của module khác, chỉ gọi qua API public của module hoặc qua event.
3. **Keycloak là nguồn duy nhất về danh tính và vai trò.** Service nghiệp vụ chỉ lưu `keycloak user id` (`sub`), không lưu mật khẩu.
4. **Chỉ thêm hạ tầng khi có nhu cầu thật**: chưa dùng Kafka, OpenSearch, service mesh.

---

## 2. Danh sách service

| # | Service | Module bên trong | DB | Replica (prod) |
|---|---|---|---|---|
| 1 | **gateway** | Định tuyến, validate JWT của Keycloak, rate limit, CORS | Redis (rate limit) | 2 |
| 2 | **customer-service** | `profile` (hồ sơ, tạo tự động khi user đăng nhập lần đầu), `address`, `wishlist`, `staff-admin` (quản lý tài khoản qua Keycloak Admin API) | `customer_db` | 2 |
| 3 | **catalog-service** | `catalog` (danh mục, brand, sản phẩm, SKU), `inventory` (tồn kho, giữ hàng), `search` (PostgreSQL full-text), `review`, `media` (presigned URL lên R2) | `catalog_db` | 2 |
| 4 | **order-service** | `cart`, `promotion` (voucher, giảm giá), `checkout` (tính tiền), `order`, `payment` (COD, VNPay, MoMo), `shipping` (GHN/GHTK) | `order_db` | 2 |
| 5 | **notification-service** | Email, SMS, Zalo ZNS cho nghiệp vụ (xác nhận đơn, giao hàng); template; lịch sử gửi | `notification_db` | 1 |
| — | **Keycloak** (thành phần hạ tầng, không tự code) | Đăng ký, đăng nhập, Google login, quên mật khẩu, xác thực email, MFA, quản lý role | `keycloak_db` | 2 |

**Thay đổi so với bản 3.0:** `identity-service` đổi thành **`customer-service`**. Toàn bộ phần xác thực (đăng ký, đăng nhập, phát token, refresh token, quên mật khẩu) chuyển sang Keycloak. Service này chỉ còn dữ liệu nghiệp vụ của khách hàng.

**Tại sao chia như vậy:**
- **catalog + inventory chung service**: tồn kho gắn chặt với SKU, gộp lại thì kiểm tra kho khi xem sản phẩm là truy vấn nội bộ.
- **cart + promotion + order + payment + shipping chung service**: cả luồng checkout nằm trong một transaction cục bộ, nên không cần saga phức tạp giữa payment và order.
- **notification tách riêng**: phụ thuộc bên thứ ba (email, SMS) hay lỗi và chậm, tách ra để không ảnh hưởng luồng đặt hàng.
- **customer tách riêng**: dữ liệu cá nhân (PII) cần phân quyền và audit riêng; là nơi duy nhất được gọi Keycloak Admin API.

Muốn xuống còn **4 service** thì gộp `customer-service` vào `order-service` (hồ sơ và sổ địa chỉ dùng chủ yếu khi checkout). Khuyến nghị giữ 5.

---

## 3. Kiến trúc tổng thể

```
Khách / Admin (trình duyệt)
     │
     ▼
Cloudflare (DNS, CDN, WAF, chống DDoS — gói Free)
     │  shop.<domain>          auth.<domain>
     ▼                              ▼
┌──────────────────────── k3s cluster (3 node VPS) ─────────────────────────┐
│  Traefik + cert-manager (Let's Encrypt)                                   │
│      │                                   │ chỉ mở /realms/*, /resources/* │
│      ▼                                   ▼                                │
│  storefront (Next.js + Auth.js) ◄──OIDC──► Keycloak (2 replica)           │
│  gồm cả /admin                           │        ▲                       │
│      │ Bearer token (phía server)        │        │ Admin API             │
│      ▼                                   │        │ (service account)     │
│  gateway (Spring Cloud Gateway) ── JWKS ─┘        │                       │
│      │ JWT, rate limit, CORS                      │                       │
│      ├──────────────┬────────────────┬────────────┘                       │
│      ▼              ▼                ▼                                    │
│  customer-svc   catalog-svc ◄─── order-svc                                │
│                     ▲  REST nội bộ (token client_credentials)             │
│      └────────── RabbitMQ (event) ──────┴──────► notification-svc         │
│                                                                           │
│  PostgreSQL (CloudNativePG: customer, catalog, order, notification,       │
│              keycloak — mỗi cái một database)                             │
│  Redis/Valkey · Argo CD · Sealed Secrets · Grafana Alloy → Grafana Cloud  │
└───────────────────────────────────────────────────────────────────────────┘
         │ backup WAL + base backup              │ ảnh sản phẩm
         ▼                                       ▼
   Cloudflare R2 (S3)                     Cloudflare R2 + CDN
```

---

## 4. Thay đổi so với bản 2.0 (13 service) để giảm chi phí

| Thành phần | Bản 2.0 | Bản 3.1 | Tiết kiệm |
|---|---|---|---|
| Service | 13 microservice | **5 service**, chia module bằng Spring Modulith | RAM, công vận hành |
| Kubernetes | Managed K8s, 2 cluster | **k3s trên 3 VPS**, 1 cluster (namespace `staging` + `prod`) | Phí control plane, số node |
| Ingress | Envoy Gateway | **Traefik** có sẵn trong k3s | Một thành phần cần cài |
| Đăng nhập | Keycloak (DB riêng) | **Keycloak** (Keycloak Operator), DB nằm chung cluster CNPG | Không tốn DB riêng |
| Message broker | Kafka managed | **RabbitMQ** trong cluster (1 node) | Chi phí lớn nhất của bản 2.0 |
| Outbox | Tự viết | **Spring Modulith Event Publication Registry** + externalize sang RabbitMQ | Ít code |
| Database | PostgreSQL managed | **CloudNativePG** trong cluster, backup PITR lên R2 | Phí DB managed |
| Tìm kiếm | OpenSearch | **PostgreSQL full-text + `unaccent` + `pg_trgm`** | 2–4 GB RAM |
| Secret | External Secrets + Vault | **Sealed Secrets** | Không cần Vault |
| Giám sát | Prometheus, Loki, Tempo tự chạy | **Grafana Alloy → Grafana Cloud (gói Free)** | ~2 GB RAM |
| Autoscale | HPA + KEDA | HPA cho gateway, catalog, order | Bỏ KEDA |
| Deploy | Argo CD + Argo Rollouts canary | Argo CD (core mode), rolling update | Bỏ Argo Rollouts |
| Ảnh | S3 + CDN | **Cloudflare R2** (miễn phí 10 GB, không tính phí băng thông) | Phí băng thông |
| Admin FE | App React riêng | **Route `/admin` trong Next.js** | 1 deployment |

---

## 5. Thiết kế Keycloak

### 5.1. Triển khai

| Hạng mục | Thiết kế |
|---|---|
| Phiên bản | Keycloak 26.x, cài bằng **Keycloak Operator** (CRD `Keycloak`) |
| Instance | Mỗi môi trường một instance: `prod` (2 replica, cache phân tán Infinispan có sẵn), `staging` (1 replica) |
| Database | Database `keycloak_db` trong cluster CloudNativePG, backup chung với các DB khác |
| Hostname | `https://auth.<domain>` (prod), `https://auth-staging.<domain>` |
| Reverse proxy | `proxy-headers=xforwarded`, `http-enabled=true` sau Traefik, TLS kết thúc ở Traefik |
| Health | Bật `health-enabled`, `metrics-enabled` (port management 9000) cho probes và Prometheus |
| Tài nguyên | `requests: 500m CPU / 1 GB`, `limits: 1.25 GB`; heap qua `JAVA_OPTS_KC_HEAP` |
| Admin console | **Không public.** Traefik chỉ route `/realms/*` và `/resources/*`; vào `/admin` bằng `kubectl port-forward` hoặc VPN (WireGuard/Tailscale) |
| Email | SMTP của Brevo (gói Free) cho xác thực email và quên mật khẩu |

### 5.2. Realm và cấu hình

**Realm `ecommerce`** (đặt cùng tên ở mọi môi trường để cấu hình giống nhau):

| Hạng mục | Cấu hình |
|---|---|
| Đăng ký | Cho phép tự đăng ký; bắt buộc xác thực email; đăng nhập bằng email |
| Social login | Identity provider **Google** (Facebook tùy chọn); tự liên kết tài khoản theo email đã xác thực |
| Ngôn ngữ | Bật i18n, mặc định `vi`, có `en` |
| Giao diện | Theme đăng nhập riêng bằng **Keycloakify** (React), đồng bộ thương hiệu với storefront |
| Mật khẩu | Tối thiểu 8 ký tự, có chữ và số, không trùng email, giữ lịch sử 3 mật khẩu gần nhất |
| Brute force | Bật: khóa tạm sau 5 lần sai, thời gian chờ tăng dần |
| MFA | OTP (TOTP) **bắt buộc với user có role `STAFF`/`ADMIN`** (conditional OTP flow); khách hàng tùy chọn |
| Token | Access token 5 phút; SSO session idle 30 phút, max 10 giờ; "Remember me" 30 ngày; bật refresh token rotation (`revokeRefreshToken`) |
| Event | Bật login event và admin event, giữ 30 ngày (phục vụ audit) |

**Role (realm role):**

| Role | Gán cho | Quyền |
|---|---|---|
| `CUSTOMER` | Mặc định khi tự đăng ký (default roles) | Mua hàng, xem đơn của mình, review |
| `STAFF` | Nhân viên (group `staff`) | Vào `/admin`: quản lý sản phẩm, tồn kho, xử lý đơn |
| `ADMIN` | Chủ shop (group `admins`) | Mọi quyền của `STAFF` + cấu hình, voucher, quản lý nhân viên, báo cáo |
| `INTERNAL` | Service account (vd. `order-service`) | Gọi `/internal/**` của service khác |

**Client:**

| Client | Loại | Flow | Dùng cho |
|---|---|---|---|
| `storefront-web` | Confidential | Authorization Code + PKCE | Next.js (Auth.js) cho cả khách hàng và `/admin` |
| `order-service` | Confidential, service account | Client Credentials | Gọi API nội bộ của catalog-service |
| `customer-service` | Confidential, service account | Client Credentials | Gọi Keycloak Admin API: chỉ có role `view-users`, `manage-users` của `realm-management` |
| `mobile-app` (sau này) | Public | Authorization Code + PKCE | App mobile |

**Client scope / mapper chung** (gán cho mọi client):
- Audience mapper: thêm `ecommerce-api` vào claim `aud`.
- Realm roles mapper: đưa role vào claim phẳng `roles` (ví dụ `["CUSTOMER"]`) cho Spring dễ đọc.
- Giữ `email`, `email_verified`, `name`, `preferred_username`.

### 5.3. Cấu hình dưới dạng code

- Toàn bộ realm (client, role, flow, mapper, password policy, identity provider) lưu thành file YAML trong `deploy/keycloak/realm/`.
- Áp dụng bằng **keycloak-config-cli** chạy thành K8s Job (Argo CD sync hook) mỗi lần cấu hình thay đổi. Không sửa tay trên admin console ở prod.
- Client secret và Google client secret lưu bằng SealedSecret, inject vào Job qua biến môi trường.
- Local dev: `docker compose` chạy Keycloak `start-dev --import-realm` với cùng file realm.

### 5.4. Đồng bộ user giữa Keycloak và customer-service

- **Tạo hồ sơ khi đăng nhập lần đầu (JIT):** khi request đầu tiên có JWT mà `sub` chưa có trong `customer_profile`, customer-service tạo hồ sơ từ claim (`email`, `name`) rồi phát event `customer.created` (để gửi email chào mừng nếu cần). Không cần event listener SPI trong Keycloak.
- **Cập nhật email/tên:** khách sửa trong trang "Tài khoản" của Keycloak (Account Console có theme riêng) hoặc qua customer-service gọi Admin API. Claim trong token luôn là nguồn chuẩn; hồ sơ cập nhật lại khi đăng nhập.
- **Quản lý nhân viên** (`/admin/staff`): customer-service gọi Admin API để tạo user, gán group `staff`, khóa hoặc mở tài khoản, buộc đặt lại mật khẩu.
- **Khóa khách hàng:** admin khóa trên `/admin`, customer-service gọi Admin API `enabled=false` và đăng xuất toàn bộ session của user đó.
- Email xác thực và quên mật khẩu do **Keycloak gửi**. notification-service chỉ gửi thông báo nghiệp vụ.

---

## 6. Frontend

| Hạng mục | Lựa chọn |
|---|---|
| Framework | Next.js 16 (App Router), TypeScript, `output: standalone` |
| Đăng nhập | **Auth.js (NextAuth v5)** với provider Keycloak, session dạng cookie mã hóa, `httpOnly` |
| UI | Tailwind CSS + shadcn/ui |
| Dữ liệu | TanStack Query; Server Components cho trang SEO |
| State | Zustand (giỏ hàng khách vãng lai) |
| Form | React Hook Form + Zod |
| Admin | Route `/admin/*`, proxy (`src/proxy.ts`, tên mới của middleware ở Next 16) kiểm tra role `STAFF`/`ADMIN` từ session |

**Luồng đăng nhập (mô hình BFF):**
```
1. Khách bấm "Đăng nhập" → Auth.js redirect sang auth.<domain> (Authorization Code + PKCE)
2. Đăng nhập (email/mật khẩu hoặc Google; nhân viên nhập thêm OTP)
3. Keycloak redirect về /api/auth/callback/keycloak → Auth.js đổi code lấy token
4. Token lưu trong session cookie đã mã hóa (httpOnly, Secure, SameSite=Lax);
   JavaScript ở trình duyệt không đọc được token
5. Server Components / Route Handlers gọi gateway kèm "Authorization: Bearer <access_token>"
6. Access token sắp hết hạn (còn < 60s) → `src/proxy.ts` gọi Auth.js, callback `jwt` dùng refresh token để làm mới;
   cookie mới được chép vào request hiện tại để Server Component dùng ngay (refresh token xoay vòng, chỉ làm mới một chỗ)
7. Đăng xuất → xóa session + RP-initiated logout (end_session_endpoint của Keycloak)
```
- API công khai (danh sách, chi tiết sản phẩm) gọi không cần token, cache bằng ISR.
- Giỏ hàng khách vãng lai được gộp vào giỏ trên server ngay sau khi đăng nhập.

---

## 7. Thiết kế từng service

### 7.1. gateway
- Spring Cloud Gateway (Server WebFlux), cấu hình **OAuth2 Resource Server**.
- `issuer-uri` = `https://auth.<domain>/realms/ecommerce` (để kiểm tra claim `iss`), còn `jwk-set-uri` trỏ thẳng địa chỉ nội bộ `http://keycloak-service.<ns>:8080/realms/ecommerce/protocol/openid-connect/certs`, nên không phải gọi vòng ra Internet. Bắt buộc `aud` chứa `ecommerce-api`.
- Route: `/api/customers/**` → customer; `/api/products/**`, `/api/categories/**`, `/api/reviews/**` → catalog; `/api/cart/**`, `/api/orders/**`, `/api/payments/**` → order; `/webhooks/**` → order (không cần JWT, xác thực bằng chữ ký).
- Phân quyền thô theo đường dẫn: `/api/admin/**` yêu cầu `STAFF` hoặc `ADMIN`; GET sản phẩm công khai.
- **Chuyển nguyên token** xuống service phía sau (không thay bằng header tự tạo).
- Rate limit bằng Redis, ví dụ: tạo đơn 10 lần/phút/user, API công khai 120 lần/phút/IP. Rate limit đăng nhập do Keycloak (brute force) lo.
- **Không route** `/internal/**` ra ngoài.

### 7.2. Bảo mật trong từng service (`common-security`)
- Mỗi service cũng là Resource Server và **tự validate JWT** (phòng thủ nhiều lớp, không tin tuyệt đối vào gateway).
- `JwtAuthenticationConverter` đọc claim `roles` thành `ROLE_CUSTOMER`, `ROLE_STAFF`, `ROLE_ADMIN`.
- `CurrentUser` lấy `sub` (UUID của Keycloak) làm `customerId`; kiểm tra quyền sở hữu tài nguyên, ví dụ khách chỉ xem đơn của mình.
- Endpoint `/internal/**` chỉ chấp nhận token client credentials của service account có realm role `INTERNAL`.
- Gọi service khác: dùng `OAuth2AuthorizedClientManager` (client credentials) gắn token vào `RestClient`; token được cache tới khi gần hết hạn.

### 7.3. customer-service
- Bảng chính: `customer_profile (id = keycloak sub, email, full_name, phone, birthday, gender, status, created_at, version)`, `address` (xóa mềm, snapshot tên tỉnh/phường, một mặc định/khách bằng partial unique index), `wishlist_item` (unique customer+sku, xóa cứng), `admin_division` (2 cấp tỉnh/phường, mã GSO zero-pad, nạp bằng Flyway repeatable `db/reference/R__admin_divisions.sql`), `admin_audit_log`.
- API: `/api/customers/me`, `/api/customers/me/addresses`, `/api/customers/me/wishlist`; công khai `/api/divisions/provinces[/{code}/wards]`; nội bộ `/internal/customers/{id}/addresses/{addressId}` (order-service lấy địa chỉ đáng tin khi checkout); admin: `/api/admin/customers` (tìm, chi tiết, khóa/mở - STAFF trở lên), `/api/admin/staff` (chỉ ADMIN).
- Gọi Keycloak Admin API bằng REST (`RestClient` + token client credentials của client `customer-service`), bọc trong một adapter riêng (`KeycloakAdminGateway`) để dễ test và mock.

### 7.4. catalog-service

**Mô hình sản phẩm đa ngành hàng:**
```
Category (cây: Thời trang > Áo > Áo thun; Trang sức > Nhẫn)
  └─ attribute_template JSONB
       Quần áo  : size[S..XXL], màu, chất liệu vải, form dáng
       Phụ kiện : màu, chất liệu, kích thước
       Trang sức: chất liệu[vàng 18K, 24K, bạc 925], size nhẫn, đá quý,
                  trọng lượng, giấy kiểm định
Brand
Product  (id, slug, name, description, brand_id, category_id, attributes JSONB,
          pricing_type FIXED|GOLD_RATE, status, seo_*, search_vector, version)
ProductVariant/SKU (id, product_id, sku_code, option_values JSONB, price,
          compare_at_price, weight, status, version)
ProductImage (product_id, variant_id nullable, url, sort_order, alt)
Review   (id, product_id, sku_id, customer_id, order_id, author_name, rating, content, image_keys, status, reject_reason)
ReviewEligibility (customer_id, product_id, sku_id, order_id, delivered_at)   — mở khi nhận order.delivered (B-29)
```

**Tồn kho:**
```
inventory_item (sku_id, on_hand, reserved, version)       -- 1 kho cho MVP
reservation    (id, order_id, sku_id, qty, status RESERVED|COMMITTED|RELEASED, expires_at)
```
- `available = on_hand - reserved`, cập nhật bằng Optimistic Locking (`@Version`).
- Job chạy mỗi phút để nhả các reservation đã hết hạn.

**Tìm kiếm (thay OpenSearch):**
- Cột `search_vector tsvector` sinh từ `unaccent(name || brand || category)`, index GIN, nên tìm được tiếng Việt không dấu.
- `pg_trgm` cho gợi ý khi gõ và chịu được lỗi chính tả nhẹ.
- Lọc facet (size, màu, chất liệu, giá) bằng index GIN trên `option_values`/`attributes`.

**API nội bộ cho order-service** (yêu cầu token client credentials có role `INTERNAL`):
- `POST /internal/skus/lookup` (lấy giá, tên, trạng thái theo danh sách SKU)
- `POST /internal/reservations` (idempotent theo `orderId`)

### 7.5. order-service

**Bảng chính:**
```
cart, cart_item                       -- giỏ của khách đã đăng nhập (khách vãng lai giữ ở FE)
voucher, voucher_usage
orders (id, code, customer_id (= keycloak sub), status, subtotal, discount, shipping_fee,
        total, shipping_address JSONB, contact_email, payment_method, idempotency_key, version)
order_item (order_id, sku_id, product_name, variant_label, unit_price, qty)   -- snapshot
order_status_history
payment (id, order_id, provider, amount, status, provider_txn_id, raw_callback JSONB)
shipment (id, order_id, carrier, tracking_code, fee, status)
```

**Luồng checkout:**
```
1. POST /api/orders (Bearer token của khách, Idempotency-Key)
2. Gọi catalog /internal/skus/lookup → lấy giá hiện tại (không tin giá client gửi lên)
3. Module promotion tính giảm giá; module shipping tính phí (GHN/GHTK API)
4. Gọi catalog /internal/reservations (idempotent theo orderId, TTL 15 phút)
     thất bại → trả lỗi "hết hàng", chưa có đơn nào được tạo
5. Transaction cục bộ: tạo order (PENDING) + payment (INIT)
     COD    → order CONFIRMED ngay
     Online → trả URL thanh toán VNPay/MoMo → AWAITING_PAYMENT
6. Webhook thanh toán (kiểm tra chữ ký, idempotent theo provider_txn_id)
     thành công → CONFIRMED | thất bại hoặc hết hạn → CANCELLED
7. Event order.confirmed / order.cancelled (qua outbox)
     catalog: COMMIT (trừ on_hand) hoặc RELEASE reservation
     notification: gửi email/Zalo cho khách
8. Nhân viên tạo vận đơn trên /admin → SHIPPING → webhook hãng vận chuyển → DELIVERED → COMPLETED
```
- Nếu bước 5 lỗi sau khi đã giữ hàng: reservation tự hết hạn theo TTL, order-service cũng gửi lệnh release. Không cần saga orchestrator.
- Email và tên người nhận được snapshot vào đơn, nên notification-service không phải gọi Keycloak.
- MVP (E3): thanh toán online dùng cổng giả `MOCK_ONLINE` (B-18) và hãng vận chuyển giả `MockCarrier` (B-19) cùng webhook ký HMAC; VNPay/MoMo, GHN/GHTK thật thay bằng bean mới ở giai đoạn Go-live (VNPay đã có `VnpayGateway` + IPN `GET /webhooks/payments/vnpay`, MoMo có `MomoGateway` + IPN `POST /webhooks/payments/momo`; bật bằng cấu hình, xem B-24, B-25). Quote trả thêm `paymentMethods` đang bật. Có thêm `POST /api/checkout/quote` để FE hiển thị bảng tính (giá hiện tại, giảm giá, lựa chọn giao hàng) trước khi đặt.
- Địa chỉ giao hàng lấy từ sổ địa chỉ qua `GET /internal/customers/{id}/addresses/{addressId}` của customer-service (B-15) hoặc khách nhập tay; đơn lưu snapshot JSONB.

Trạng thái đơn: `PENDING → AWAITING_PAYMENT → CONFIRMED → PACKING → SHIPPING → DELIVERED → COMPLETED`, nhánh lỗi: `CANCELLED`, `RETURN_REQUESTED → RETURNED → REFUNDED`.

### 7.6. notification-service
- Consume event từ RabbitMQ, render template (Thymeleaf), gửi qua Brevo/Resend (email), eSMS hoặc Zalo ZNS.
- Chỉ gửi thông báo nghiệp vụ (đơn hàng, giao hàng, khuyến mãi). Email xác thực và quên mật khẩu do Keycloak gửi.
- Lưu `notification_log` (idempotent theo `event_id`), retry với backoff, lỗi nhiều lần thì đẩy vào dead-letter queue.
- MVP (E4): chỉ kênh email qua SMTP (`NotificationChannel` strategy, Mailpit local); template `templates/mail/*.html` dùng chung `_layout.html`; tiêu đề lấy từ `messages*.properties`. Zalo ZNS/SMS thêm bằng bean kênh mới.

---

## 8. Giao tiếp và event

| Kiểu | Dùng khi | Công nghệ |
|---|---|---|
| Đồng bộ | order cần giá/kho từ catalog; customer gọi Keycloak Admin API | `RestClient` / `@HttpExchange` + token client credentials + Resilience4j (timeout 2s, retry 2 lần, circuit breaker) |
| Bất đồng bộ | Thay đổi trạng thái, thông báo | Spring Modulith events → externalize sang **RabbitMQ** (topic exchange) |

**Event chính:**

| Event | Phát từ | Nhận bởi |
|---|---|---|
| `customer.created` | customer | notification (email chào mừng, tùy chọn) |
| `order.confirmed`, `order.cancelled` | order | catalog, notification |
| `order.shipped`, `order.delivered` | order | notification, catalog (cho phép review; `order.delivered` mang `lines` từ E7.1) |
| `product.price-changed` (tùy chọn) | catalog | order (làm mới giỏ hàng) |

- Bên phát: event được lưu vào bảng `event_publication` (Spring Modulith) trong cùng transaction với dữ liệu, sau đó mới gửi đi. Đây chính là outbox, không cần tự viết.
- Bên nhận: idempotent bằng bảng `processed_event`; dùng dead-letter exchange.
- Khi lượng event lớn hoặc cần replay thì đổi sang Kafka. Spring Modulith hỗ trợ cả hai nên chỉ đổi cấu hình externalization.

---

## 9. Hạ tầng Kubernetes chi phí thấp

### 9.1. Cluster

| Hạng mục | Lựa chọn |
|---|---|
| Distribution | **k3s**, 3 node, cả 3 đều làm server (embedded etcd), nên control plane có HA |
| Node | 3 VPS, mỗi node **4 vCPU / 8 GB** (tối thiểu) hoặc **4 vCPU / 12–16 GB** (khuyến nghị khi có Keycloak, xem 9.2) |
| Nhà cung cấp | VPS trong nước (Viettel IDC, VNPT, BizFly, ...) để độ trễ tới khách Việt Nam thấp; hoặc Hetzner/DigitalOcean nếu muốn rẻ hoặc cần managed |
| Ingress | Traefik (có sẵn) + cert-manager |
| Lưu trữ | `local-path` (có sẵn) cho PostgreSQL; nhân bản dữ liệu do CloudNativePG lo (primary + replica nằm trên 2 node khác nhau) |
| Namespace | `prod`, `staging` (mỗi môi trường chứa luôn dữ liệu `pg-rw`/`rabbitmq`/`valkey` và Keycloak + Keycloak Operator của nó), `platform` (cert-manager, sealed-secrets, CloudNativePG + plugin Barman Cloud, alloy), `argocd`. Manifest ở `deploy/`, xem B-26 |

**Phương án thay thế:** managed K8s có control plane miễn phí (DigitalOcean DOKS, Linode LKE, Vultr VKE). Đỡ công vận hành nhưng đắt hơn khoảng 1,5–2 lần.

### 9.2. Ngân sách RAM (ước tính)

| Thành phần | Prod | Staging |
|---|---|---|
| gateway | 2 × 384 MB | 1 × 384 MB |
| customer-service | 2 × 512 MB | 1 × 512 MB |
| catalog-service | 2 × 768 MB | 1 × 768 MB |
| order-service | 2 × 768 MB | 1 × 768 MB |
| notification-service | 1 × 384 MB | 1 × 384 MB |
| storefront (Next.js) | 2 × 256 MB | 1 × 256 MB |
| **Keycloak** | **2 × 1 GB** | **1 × 768 MB** |
| PostgreSQL (CNPG) | 2 × 1,5 GB | 1 × 512 MB |
| Redis/Valkey | 256 MB | 128 MB |
| RabbitMQ | 512 MB | 256 MB |
| **Tổng** | **~11,8 GB** | **~4,7 GB** |
| Platform (Traefik, cert-manager, Argo CD core, Sealed Secrets, Alloy, Keycloak Operator) | ~1,7 GB | |

- Tổng khoảng **18 GB**. Với 3 node 8 GB (24 GB) thì vẫn chạy được, nhưng khi 1 node chết chỉ còn 16 GB, không đủ chứa hết. Phương án xử lý: **tắt staging (scale về 0) khi có sự cố**, hoặc dùng **node 12–16 GB** (chênh khoảng 300–600k VND/tháng).
- Muốn tiết kiệm RAM hơn nữa thì build **GraalVM native image** cho các service Spring Boot (mỗi service khoảng 150–250 MB), hoặc chạy Keycloak prod 1 replica (chấp nhận gián đoạn đăng nhập vài chục giây khi restart).

### 9.3. Chi phí hằng tháng (ước tính, cần kiểm tra giá thực tế)

| Hạng mục | Chi phí |
|---|---|
| 3 VPS 4 vCPU / 8–16 GB | ~1,2–2,5 triệu VND (trong nước) / ~0,7–1,2 triệu (Hetzner) |
| Domain | ~25k VND (~300k/năm) |
| Keycloak | 0 (mã nguồn mở, chỉ tốn RAM) |
| Cloudflare (CDN, DNS, WAF), R2 (≤10 GB), Grafana Cloud Free, Brevo Free (email nghiệp vụ + SMTP cho Keycloak), GitHub Actions/GHCR | 0 |
| **Tổng** | **~1,2–2,5 triệu VND/tháng** (bản 2.0: ~10–35 triệu) |

Phí giao dịch của cổng thanh toán (VNPay/MoMo) tính theo đơn, không nằm trong bảng này.

### 9.4. Chuẩn cho mỗi Spring Boot service (Helm chart chung `spring-service`)

| Hạng mục | Chuẩn |
|---|---|
| Image | Jib, base JRE tối giản, chạy non-root, tag theo git SHA |
| JVM | `-XX:MaxRAMPercentage=75`, virtual threads bật |
| Probes | `startupProbe`, `liveness` → `/actuator/health/liveness`, `readiness` → `/actuator/health/readiness` |
| Tắt máy | `server.shutdown=graceful`, `preStop` sleep 10s |
| Resource | Đặt `requests` CPU/memory; `limits` chỉ cho memory |
| Sẵn sàng | Prod: 2 replica (trừ notification), `topologySpreadConstraints` theo node, PDB `minAvailable: 1` |
| Autoscale | HPA (CPU 70%, max 4 replica) cho gateway, catalog, order |
| Mạng | NetworkPolicy default-deny (k3s có sẵn network policy controller). Cho phép: Traefik → storefront, gateway, Keycloak; gateway → các service; order → catalog `/internal`; mọi service → Keycloak (lấy JWKS và token) |
| Config/Secret | ConfigMap theo môi trường; secret (client secret Keycloak, mật khẩu DB) dùng SealedSecret |
| Migration | Flyway chạy khi service khởi động; chỉ làm thay đổi tương thích ngược |

### 9.5. Dữ liệu và backup

- **CloudNativePG**: 1 cluster PostgreSQL gồm 1 primary và 1 replica (trên 2 node khác nhau), tự failover. Mỗi service và Keycloak một database, user riêng.
- **Backup**: base backup hằng ngày + WAL archive liên tục lên **Cloudflare R2**, nên có PITR. Giữ 14 ngày. `keycloak_db` được backup cùng, gồm user, mật khẩu đã hash và cấu hình realm.
- **Thử restore** sang namespace `staging` mỗi tháng một lần (gồm cả Keycloak).
- Redis chỉ chứa dữ liệu có thể mất (cache, rate limit). Giỏ hàng của khách đã đăng nhập lưu trong PostgreSQL.
- RabbitMQ dùng queue durable. Event gốc vẫn còn trong `event_publication` nên có thể gửi lại.

---

## 10. CI/CD

```
Pull Request → GitHub Actions
  1. Build + unit test + Spring Modulith verify (kiểm tra ranh giới module)
  2. Integration test (Testcontainers: PostgreSQL, RabbitMQ, Redis, Keycloak)
  3. Build image bằng Jib → quét Trivy → push GHCR (tag = git SHA)
  4. Build theme Keycloakify (nếu có thay đổi) → đóng gói vào image Keycloak tùy biến
Merge main → cập nhật tag trong deploy/environments/staging → Argo CD sync staging
             (Job keycloak-config-cli áp dụng cấu hình realm)
Release    → PR cập nhật tag prod → approve → Argo CD sync prod
```
- Chỉ build service nào có thay đổi (lọc theo path).
- Rollback: revert commit trong `deploy/`.
- Test tích hợp bảo mật dùng **Testcontainers Keycloak** (`dasniko/testcontainers-keycloak`) import cùng file realm, lấy token thật để gọi API.
- Dev local: `docker compose` cho PostgreSQL, Redis, RabbitMQ, MinIO, Keycloak (import realm); service chạy từ IDE.

---

## 11. Bảo mật

| Lớp | Biện pháp |
|---|---|
| Biên | Cloudflare proxy (ẩn IP VPS), WAF, chỉ mở port 80/443 cho dải IP của Cloudflare |
| VPS | SSH bằng key, tắt đăng nhập bằng mật khẩu, firewall (ufw), cập nhật bảo mật tự động |
| Xác thực | Keycloak OIDC; Authorization Code + PKCE; access token 5 phút, refresh token xoay vòng; brute force detection |
| Nhân viên | Bắt buộc OTP; tài khoản tạo qua `/admin/staff`, không cho tự đăng ký role `STAFF` |
| Keycloak admin | Console không public; tài khoản admin master realm chỉ dùng khi khẩn cấp, mật khẩu mạnh + OTP |
| Token ở FE | Lưu trong cookie mã hóa `httpOnly` của Auth.js; không đặt trong `localStorage` |
| Phân quyền | `CUSTOMER`, `STAFF`, `ADMIN`; gateway lọc theo đường dẫn, service kiểm tra quyền sở hữu tài nguyên |
| Nội bộ | NetworkPolicy; `/internal/**` không qua gateway và yêu cầu token client credentials có role `INTERNAL` |
| Least privilege | Service account `customer-service` chỉ có `view-users`, `manage-users`; không cấp `realm-admin` |
| Secret | Sealed Secrets; client secret xoay vòng định kỳ |
| Thanh toán | Không lưu thông tin thẻ; kiểm tra chữ ký webhook; idempotent theo mã giao dịch |
| Pod | non-root, read-only root filesystem, bỏ hết capability |
| Audit | Login event và admin event của Keycloak đẩy log về Grafana Cloud |

---

## 12. Giám sát, logging và audit

### 12.1. Metrics và alert (gói Free của Grafana Cloud)

- **Grafana Alloy** trong cluster gom metrics (Micrometer/Prometheus + metrics của Keycloak), logs (JSON stdout) và traces (OTLP), rồi đẩy lên Grafana Cloud.
- Dashboard: JVM, HTTP p95, lỗi 5xx, số đơn/giờ, tỉ lệ thanh toán thành công, queue RabbitMQ, dung lượng PostgreSQL, **số lần đăng nhập thành công/thất bại, số user bị khóa** (Keycloak).
- Alert gửi về Telegram/Email khi: 5xx > 2% trong 5 phút, p95 checkout > 1s, **đăng nhập thất bại tăng đột biến** (dấu hiệu tấn công), Keycloak không lấy được kết nối DB, node RAM > 85%, backup thất bại, cert sắp hết hạn.
- Thêm **Uptime Kuma** hoặc giám sát uptime của Grafana Cloud để kiểm tra từ bên ngoài, gồm cả `https://auth.<domain>/realms/ecommerce/.well-known/openid-configuration`.

### 12.2. Logging

**Luồng thu thập:**
```
Pod (log JSON ra stdout)
   → k3s/containerd ghi file log trên node (xoay vòng 10 MB × 5 file)
   → Grafana Alloy (DaemonSet) đọc log, parse JSON, gắn label
   → Grafana Cloud Loki (gói Free: 50 GB/tháng, giữ 14 ngày)
   → Grafana Explore: tìm log, nhảy qua lại giữa log ↔ trace (Tempo)
```

**Nguồn log:**

| Nguồn | Định dạng / cấu hình |
|---|---|
| 5 service Spring Boot | Structured logging có sẵn của Spring Boot: `logging.structured.format.console=ecs` |
| storefront (Next.js) | `pino` ghi JSON ra stdout |
| Keycloak | `log-console-output=json`; bật event listener `jboss-logging` để login event và admin event cũng ra log |
| Traefik | Access log JSON (bỏ qua đường dẫn health check) |
| PostgreSQL (CNPG) | Log JSON có sẵn; bật slow query `log_min_duration_statement=500ms` |
| RabbitMQ | Log console JSON |
| Kubernetes events | Alloy thu `kubernetes events` (OOMKilled, CrashLoopBackOff, pod bị evict) |

**Trường chuẩn trong mỗi dòng log của service:**

| Trường | Ví dụ | Ghi chú |
|---|---|---|
| `@timestamp`, `log.level`, `log.logger`, `message` | | Có sẵn trong format ECS |
| `service.name`, `service.version`, `service.environment` | `order-service`, `3f2a9c1`, `prod` | Cấu hình qua `spring.application.name` và biến môi trường |
| `trace.id`, `span.id` | | Micrometer Tracing tự gắn vào MDC |
| `user.id` | Keycloak `sub` | Filter trong `common-security` đưa vào MDC |
| `order.id`, `payment.provider` | | Gắn vào MDC trong các luồng nghiệp vụ quan trọng |
| `error.type`, `error.stack_trace` | | Chỉ khi có exception |

**Correlation (lần theo một request xuyên các service):**
- Gateway tạo trace theo chuẩn W3C `traceparent`. `RestClient` và RabbitMQ (bật `observation-enabled` cho listener và template) truyền tiếp xuống service sau, kể cả qua event.
- Mọi response lỗi có trường `traceId` trong body (`common-web`) và header `X-Trace-Id`. Khách báo lỗi kèm mã này là tìm được ngay toàn bộ log của request.
- Webhook thanh toán và vận chuyển: gắn `order.id` và `provider_txn_id` vào MDC để tra theo mã đơn.

**Ghi log những gì:**

| Loại | Mức | Nội dung |
|---|---|---|
| Access log | INFO | Một dòng mỗi request tại gateway: method, route, status, latency, `user.id`, IP (lấy từ `CF-Connecting-IP`) |
| Nghiệp vụ | INFO | Tạo đơn, đổi trạng thái đơn, nhận/kiểm tra webhook, giữ/nhả hàng, áp voucher |
| Tích hợp bên ngoài | INFO/WARN | Gọi VNPay, MoMo, GHN, GHTK, Brevo: endpoint, status, latency, số lần retry (không log body chứa dữ liệu nhạy cảm) |
| Bảo mật | WARN | Token không hợp lệ, 403, chữ ký webhook sai, vượt rate limit |
| Lỗi | ERROR | Chỉ log stack trace **một lần** tại `@RestControllerAdvice` hoặc error handler của listener, không log lặp ở mọi tầng |
| Debug | DEBUG | Tắt ở prod; bật tạm cho một package qua `/actuator/loggers` (chỉ gọi được từ trong cluster) mà không cần restart |

**Không bao giờ log:**
- Mật khẩu, access/refresh token, client secret, header `Authorization`, cookie session.
- Thông tin thẻ, chữ ký và secret key của cổng thanh toán.
- Dữ liệu cá nhân đầy đủ: email, số điện thoại, địa chỉ phải được che, ví dụ `n***@gmail.com`, `09******89`.

Cách đảm bảo: dùng `StructuredLoggingJsonMembersCustomizer` để che các trường nhạy cảm; các object nhạy cảm override `toString()`; rule review code không log nguyên DTO request/response. Điều này cũng để tuân thủ quy định bảo vệ dữ liệu cá nhân (Nghị định 13/2023/NĐ-CP và Luật Bảo vệ dữ liệu cá nhân).

**Label trong Loki (giữ ít để không vượt hạn mức):**
- Label: `namespace`, `app`, `env`, `level`.
- `trace.id`, `user.id`, `order.id` **không làm label** (số lượng giá trị quá lớn). Đưa chúng vào structured metadata hoặc tìm bằng bộ lọc JSON.

**Kiểm soát dung lượng:**
- Ước tính với ~1.000 đơn/ngày: khoảng 1–3 GB log/tháng, trong hạn mức Free.
- Alloy loại bỏ log health check và log DEBUG ở prod. Staging chỉ giữ WARN trở lên.
- Đặt cảnh báo khi đã dùng 80% hạn mức tháng.

**Ví dụ truy vấn LogQL thường dùng:**
```logql
# Toàn bộ log của một request (khách gửi mã traceId)
{env="prod"} | json | trace_id="4bf92f3577b34da6a3ce929d0e0e4736"

# Lỗi theo service trong 1 giờ
sum by (app) (count_over_time({env="prod", level="ERROR"}[1h]))

# Lịch sử xử lý một đơn hàng
{env="prod", app="order-service"} | json | order_id="ORD-20261019-0001"

# Webhook thanh toán có chữ ký sai
{env="prod", app="order-service"} |= "PAYMENT_SIGNATURE_INVALID"

# Đăng nhập thất bại trên Keycloak
{env="prod", app="keycloak"} | json | type="LOGIN_ERROR"
```

**Alert dựa trên log** (Grafana Alerting, gửi về Telegram):
- Số log `ERROR` của một service > 20 trong 5 phút.
- Có `PAYMENT_SIGNATURE_INVALID` hoặc lỗi gọi cổng thanh toán liên tục.
- `LOGIN_ERROR` của Keycloak tăng đột biến, ví dụ > 50 trong 5 phút.
- Kubernetes event `OOMKilled` hoặc `CrashLoopBackOff`.

**Khi Grafana Cloud không truy cập được hoặc hết hạn mức:**
- Xem trực tiếp bằng `kubectl logs` hoặc `stern` (theo dõi log nhiều pod cùng lúc), ví dụ `stern -n prod order-service`.
- Log trên node còn giữ được vài ngày gần nhất nhờ cơ chế xoay vòng của k3s.

**Dev local:** profile `dev` in log dạng text dễ đọc (không JSON), mức DEBUG cho package của dự án.

### 12.3. Audit log nghiệp vụ (lưu lâu dài)

Log trong Loki chỉ giữ 14 ngày, không dùng làm bằng chứng. Các thao tác cần truy vết lâu dài được lưu vào DB:

| Dữ liệu | Lưu ở đâu | Thời gian giữ |
|---|---|---|
| Thay đổi trạng thái đơn hàng | `order_status_history` (ai đổi, lúc nào, lý do) | Vĩnh viễn |
| Giao dịch và callback thanh toán | `payment` (`raw_callback` đã che dữ liệu nhạy cảm) | Tối thiểu 10 năm (chứng từ kế toán) |
| Thay đổi sản phẩm, giá, tồn kho | **Hibernate Envers** (bảng `*_aud`) trong catalog-service | 2 năm |
| Thao tác của nhân viên (sửa đơn, hoàn tiền, khóa khách, tạo voucher) | Bảng `admin_audit_log` trong từng service: `actor_id`, `action`, `target`, `before`/`after` JSONB, `trace_id` | 2 năm |
| Đăng nhập và thao tác quản trị Keycloak | Keycloak event store (30 ngày) + log ra Loki | 30 ngày; xuất định kỳ lên R2 nếu cần lâu hơn |

Các cột `created_by`, `updated_by`, `created_at`, `updated_at` dùng Spring Data JPA Auditing, lấy `sub` của user hiện tại từ JWT.

---

## 13. Tech stack

| Hạng mục | Công nghệ |
|---|---|
| Backend | Java 25 (LTS), Spring Boot 4.x, Spring Modulith, Spring Cloud Gateway |
| Identity | **Keycloak 26.x** (Keycloak Operator), keycloak-config-cli, Keycloakify (theme) |
| Bảo mật | Spring Security OAuth2 Resource Server + OAuth2 Client (client credentials) |
| Persistence | Spring Data JPA, Flyway, MapStruct |
| Messaging | Spring AMQP (RabbitMQ), Spring Modulith event externalization |
| Gọi service | `RestClient` / `@HttpExchange`, Resilience4j |
| Cache | Spring Cache + Caffeine (local) / Redis |
| Logging/Audit | Spring Boot structured logging (ECS JSON), Micrometer Tracing, Grafana Alloy + Loki, Hibernate Envers, Spring Data JPA Auditing |
| API docs | springdoc-openapi (cấu hình OAuth2 để thử API bằng token Keycloak) |
| Test | JUnit 5, Mockito, Testcontainers (kể cả Keycloak), Spring Modulith test |
| Build/Deploy | Maven multi-module, Jib, Helm, Argo CD, k3s |
| Frontend | Next.js 16, TypeScript, Auth.js, Tailwind, shadcn/ui, TanStack Query |

Nếu team chưa sẵn sàng với Spring Boot 4 thì dùng Java 21 + Spring Boot 3.5 (Spring Modulith 1.x). Chốt một bộ version cho toàn dự án.

---

## 14. Cấu trúc repo

```
ecommerce/
├── backend/
│   ├── pom.xml                       # parent, quản lý version
│   ├── common/
│   │   ├── common-web/               # error response, exception handler
│   │   ├── common-security/          # resource server, role converter, CurrentUser, client credentials
│   │   └── common-events/            # event contract có version
│   ├── gateway/
│   ├── customer-service/
│   │   └── src/main/java/.../customer/{profile,address,wishlist,staffadmin}/
│   ├── catalog-service/
│   │   └── src/main/java/.../catalog/{product,inventory,search,review,media}/
│   ├── order-service/
│   │   └── src/main/java/.../order/{cart,promotion,checkout,order,payment,shipping}/
│   └── notification-service/
├── frontend/
│   ├── storefront/                   # Next.js + Auth.js (gồm /admin)
│   └── keycloak-theme/               # Keycloakify: theme login/account
├── deploy/
│   ├── charts/spring-service/        # Helm chart dùng chung
│   ├── environments/{staging,prod}/  # values từng service
│   ├── keycloak/
│   │   ├── keycloak.yaml             # CR Keycloak cho từng môi trường
│   │   ├── realm/ecommerce.yaml      # cấu hình realm (keycloak-config-cli)
│   │   └── config-job.yaml           # Job áp dụng realm
│   ├── data/                         # CNPG cluster, Redis, RabbitMQ
│   ├── platform/                     # cert-manager, sealed-secrets, alloy, keycloak-operator
│   └── argocd/                       # app-of-apps
├── infra/
│   └── k3s/                          # script/Ansible dựng VPS + k3s
├── local/
│   ├── docker-compose.yml
│   └── keycloak/ecommerce-realm.json # realm cho môi trường dev
├── .github/workflows/
└── docs/
```

---

## 15. Lộ trình

| Giai đoạn | Nội dung | Thời gian ước tính* |
|---|---|---|
| **0. Nền tảng** | Monorepo, parent pom, common libs; docker-compose local (có Keycloak); dựng 3 VPS + k3s; cert-manager, Argo CD, Sealed Secrets, CNPG, backup R2; **Keycloak Operator + realm as code**; Helm chart chung; CI | 2–3 tuần |
| **1. MVP** | Đăng nhập Keycloak (email + Google) với Auth.js; customer (hồ sơ JIT, địa chỉ); catalog + SKU + tồn kho + tìm kiếm; cart, checkout COD; admin quản lý sản phẩm/đơn (role `STAFF`, có OTP); storefront | 6–8 tuần |
| **2. Go-live** | VNPay/MoMo, GHN/GHTK, voucher, notification email/Zalo; theme đăng nhập Keycloakify tiếng Việt; load test (gồm cả đăng nhập); thử restore; mở bán | 3–4 tuần |
| **3. Tăng trưởng** | Review, wishlist, flash sale (giữ hàng bằng Redis), giá trang sức theo giá vàng, báo cáo doanh thu; app mobile (client `mobile-app`) | 4–6 tuần |

\*Ước tính cho team 1–2 BE và 1 FE; phần DevOps do BE kiêm nhiệm.

---

## 16. Lộ trình mở rộng khi đơn hàng tăng

| Dấu hiệu | Hành động |
|---|---|
| CPU/RAM node thường xuyên > 70% | Thêm node hoặc nâng cấu hình VPS |
| PostgreSQL thành điểm nghẽn | Tách DB của order (và Keycloak) sang instance riêng; thêm read replica cho catalog |
| Đăng nhập chậm vào giờ cao điểm | Tăng replica Keycloak, chỉnh cache Infinispan |
| Tìm kiếm chậm, cần gợi ý thông minh | Tách module `search` thành search-service + OpenSearch |
| Flash sale lớn | Tách module `inventory`, giữ hàng bằng Redis Lua |
| Event nhiều, cần replay | Đổi RabbitMQ sang Kafka (chỉ đổi cấu hình externalization) |
| Team > 5 BE | Tách tiếp theo module (payment, promotion) và chuyển dần sang kiến trúc của bản 2.0 |

Vì module đã tách sẵn package, schema và chỉ giao tiếp qua API/event, việc tách service chủ yếu là chuyển code và đổi chỗ gọi.

---

## 17. Checklist trước khi go-live

- [ ] Mỗi service có probes, graceful shutdown, PDB (prod), NetworkPolicy
- [ ] `spring-modulith` verify pass (không có module gọi chéo trái phép)
- [ ] Keycloak: admin console không truy cập được từ Internet; brute force bật; OTP bắt buộc cho `STAFF`/`ADMIN`; SMTP gửi mail xác thực được
- [ ] Realm được quản lý hoàn toàn bằng code; staging và prod giống nhau (trừ secret, URL)
- [ ] Đã test: token hết hạn, refresh token bị thu hồi, đăng xuất ở mọi thiết bị, khóa user thì mất quyền ngay ở lần refresh tiếp theo
- [ ] Service từ chối token có `iss` hoặc `aud` sai; `/internal/**` từ chối token của khách
- [ ] Backup PostgreSQL (gồm `keycloak_db`) lên R2 chạy đều; đã restore thử thành công
- [ ] Webhook thanh toán test đủ trường hợp: trùng, sai chữ ký, thanh toán trễ sau khi đơn đã hết hạn
- [ ] Reservation hết hạn được nhả đúng; không bán âm kho khi 2 khách mua cùng lúc
- [ ] Load test checkout và đăng nhập ở staging đạt gấp 3 lần tải dự kiến
- [ ] Alert gửi tới đúng người; có runbook xử lý khi node chết, DB failover, Keycloak không lên
- [ ] Log mọi service là JSON, có `trace.id` và `user.id`; response lỗi trả `traceId`; tra được một request xuyên gateway → order → catalog → notification
- [ ] Đã kiểm tra log không chứa mật khẩu, token, header `Authorization`, dữ liệu cá nhân chưa che
- [ ] Alert từ log (ERROR tăng, chữ ký webhook sai, đăng nhập thất bại tăng) đã bắn thử thành công
- [ ] Audit log của thao tác nhân viên và lịch sử giá sản phẩm đã ghi đúng
- [ ] VPS chỉ nhận traffic từ Cloudflare; SSH chỉ dùng key
- [ ] SEO: sitemap, robots.txt, structured data Product, ảnh có alt

---

## 18. Thông tin cần chốt

- Nhà cung cấp VPS, cấu hình node 8 GB hay 12–16 GB (xem mục 9.2).
- Social login cần những gì ngoài Google (Facebook; Zalo phải viết identity provider tùy biến).
- Cổng thanh toán và hãng vận chuyển dùng lúc ra mắt.
- Có cần app mobile không.
- Số sản phẩm/SKU và số đơn/ngày dự kiến.
