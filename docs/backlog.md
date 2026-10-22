# Backlog

Nguồn việc duy nhất khi chạy tự động. Lấy **task đầu tiên chưa tick** theo thứ tự, làm theo `/ecom-feature`, tick khi tiêu chí hoàn thành đạt và đã merge vào `main`. Gặp `⏸ CHECKPOINT` thì dừng, báo cáo, chờ chủ shop.

Quy ước mã: `E<epic>.<n>`. Tiêu chí hoàn thành phải đo được (endpoint trả đúng, test pass, trang hiển thị).

---

## E0. Nền tảng (đã xong phần lớn)

- [x] E0.1 Monorepo, common libs, 5 service khởi động được, `mvnw verify` xanh
- [x] E0.2 docker-compose local (PostgreSQL, Valkey, RabbitMQ, Keycloak realm, Mailpit)
- [x] E0.3 Helm chart dùng chung + values staging/prod
- [x] E0.4 Cấu hình Claude Code (`.claude/`), rules, skills, hook
- [x] E0.5 Storefront: token thiết kế (màu, font, cỡ chữ) trong `globals.css`, layout gốc có header/footer theo mẫu đã chốt, trang chủ tĩnh đúng bố cục mẫu. Tiêu chí: `npm run build` xanh, trang chủ render đủ các khối của mẫu với dữ liệu giả
- [x] E0.7 (nhỏ) JVM test thoát chậm 30s: context Spring nằm trong cache tới lúc JVM thoát, khi đó container PostgreSQL đã dừng nên Spring Modulith (`DefaultEventPublicationRegistry.destroy`) chờ Hikari 30s. Sửa bằng `@DirtiesContext` trên các test Testcontainers; `verify` toàn bộ 3 phút 11 giây, không còn surefire kill fork
- [x] E0.6 Kiểm chứng hạ tầng local: `docker compose up` chạy, realm import được, lấy được token của `customer@example.com` bằng client `dev-cli` (thêm client này, bật password grant chỉ cho dev), 5 service khởi động với DB thật. Tiêu chí: `curl` qua gateway tới `/actuator/health` của từng service trả `UP`

## E1. catalog-service

- [x] E1.1 Module `product`: migration + entity `category` (cây, `attribute_template` JSONB), `brand`, `product`, `product_variant`, `product_image`; `AuditableEntity`, xóa mềm, Envers cho product/variant. API công khai: `GET /api/categories` (cây), `GET /api/products?category=&page=&size=&sort=` (projection card: tên, slug, giá, ảnh chính, màu), `GET /api/products/{slug}` (chi tiết + variant + ảnh). Tiêu chí: test service + `@DataJpaTest` Testcontainers, seed 12 sản phẩm mẫu bằng Flyway `R__seed_dev.sql` chỉ profile dev
- [x] E1.2 Module `product` admin: `POST/PUT/DELETE /api/admin/catalog/categories|brands|products|variants`, upload thứ tự ảnh, cập nhật nhận `version`. Tiêu chí: `@WebMvcTest` với `jwt()` role STAFF pass, CUSTOMER bị 403
- [x] E1.3 Module `inventory`: `inventory_item`, `reservation`; API nội bộ `POST /internal/skus/lookup`, `POST /internal/reservations` (idempotent theo orderId, TTL 15 phút), `POST /internal/reservations/{orderId}/commit|release`; job nhả hết hạn; admin `GET/PUT /api/admin/inventory`. Tiêu chí: test đồng thời 2 reservation trên 1 SKU không bán âm
- [x] E1.4 Module `search` (gộp vào module `product` dưới dạng `ProductSearchRepository`, không tách module riêng vì cùng bảng): cột `search_vector` + `unaccent`/`pg_trgm`, `GET /api/products?q=` tìm không dấu, facet đếm theo thuộc tính (`GET /api/products/facets?category=`). Tiêu chí: tìm "nhan bac" ra "Nhẫn bạc"
- [x] E1.5 Consumer `order.confirmed.v1` → commit reservation; `order.cancelled.v1` → release; idempotent qua `processed_event`. Tiêu chí: test gửi trùng event 2 lần chỉ trừ kho 1 lần

⏸ **CHECKPOINT C1**: demo catalog qua Swagger + storefront đọc được danh mục/sản phẩm thật

## E2. customer-service

- [x] E2.1 Module `profile`: JIT tạo hồ sơ từ JWT lần đầu, `GET/PUT /api/customers/me`, event `customer.created.v1`; admin `GET /api/admin/customers[/{id}]` (tìm theo email/tên/SĐT)
- [x] E2.2 Module `address`: CRUD sổ địa chỉ (xóa mềm, 1 mặc định/khách, tối đa `ecommerce.address.max-per-customer`), bảng `admin_division` 2 cấp (34 tỉnh / 3.321 phường, Flyway `db/reference/R__admin_divisions.sql`, mã GSO zero-pad), `GET /api/divisions/provinces[/{code}/wards]` công khai, `GET /internal/customers/{id}/addresses/{addressId}` cho order-service
- [x] E2.3 Module `wishlist`: `GET/PUT/DELETE /api/customers/me/wishlist[/{skuId}]`, idempotent, xóa cứng; không gọi catalog kiểm tra SKU (storefront ẩn mục không còn bán)
- [x] E2.4 Module `staffadmin`: adapter `KeycloakAdminGateway` (RestClient + client_credentials, mock trong test), `POST /api/admin/customers/{id}/lock|unlock` (STAFF/ADMIN; Keycloak enabled=false + logout mọi phiên + `admin_audit_log`), `GET/POST /api/admin/staff`, `POST /api/admin/staff/{id}/enable|disable` (chỉ ADMIN; tạo user vào group `staff`, mật khẩu tạm bắt đổi). Chưa smoke test thật với Keycloak Admin API → làm ở E5.7 admin UI

## E3. order-service

- [x] E3.1 Module `cart`: giỏ server cho khách đăng nhập (`GET /api/cart`, `PUT /api/cart/items`, `DELETE /api/cart/items/{skuId}`, `POST /api/cart/merge`), dòng = (sku, khắc tên), giá/tồn tra lại từ catalog qua `CatalogClient` mỗi lần xem, `issues` cảnh báo hết hàng/ngừng bán
- [x] E3.2 Module `promotion`: `voucher` PERCENT/FIXED (trần, tối thiểu, hạn, số lượt, lượt/khách), `voucher_usage`, `POST /api/vouchers/validate`; admin CRUD `/api/admin/vouchers` (chỉ ADMIN, xóa mềm, audit)
- [x] E3.3 Module `shipping`: `ShippingCarrier` strategy + `CarrierRegistry` (bean thật ghi đè `MockCarrier` cùng mã), phí GHN 25k/GHTK 18k, freeship ≥ 500k (B-07), `shipment`, webhook `POST /webhooks/shipping/mock` (HMAC) → Spring event `ShipmentStatusChanged` → orders DELIVERED. GHN/GHTK thật ở E6
- [x] E3.4 Module `checkout` + `orders`: `POST /api/checkout/quote`, `POST /api/orders` (Idempotency-Key, 201/200 replay) theo luồng 7.5; địa chỉ từ sổ (gọi customer `/internal`) hoặc nhập tay; `order_status_history`; state machine trong `OrderStatus`; mã `ORD-yymmdd-NNNN`; `GET /api/orders/me|{code}`, `POST /api/orders/{code}/cancel`; event confirmed/cancelled/shipped/delivered qua outbox; job hủy đơn online quá 15 phút (B-20)
- [x] E3.5 Module `payment`: `PaymentGateway` strategy, `CodGateway`, `MockOnlineGateway` (payUrl `/thanh-toan/mock/{orderId}` + webhook `POST /webhooks/payments/mock` ký HMAC-SHA256), idempotent theo `provider_txn_id`, lệch số tiền → FAILED. VNPay/MoMo ở E6
- [x] E3.6 Admin đơn hàng: `GET /api/admin/orders?status&q`, `POST /{code}/status|ship|cancel`, `POST /api/admin/orders/ship` hàng loạt (mỗi đơn một transaction), `admin_audit_log` (recorder dùng chung trong common-jpa)

- [x] E3.7 Sửa luồng event end-to-end: converter AMQP của consumer tin các package trong `EventRoutes.EVENT_PACKAGES` (Spring AMQP so khớp đúng tên package, không tính package con; trước đó mọi message vào DLQ); hủy đơn đã chốt kho thì catalog hoàn `on_hand` (B-22). Đã kiểm thật: đặt COD qua gateway → COMMITTED, tồn trừ đúng; hủy → hoàn tồn

⏸ **CHECKPOINT C2**: đặt được đơn COD end-to-end bằng Swagger/curl, kho trừ đúng, email xác nhận vào Mailpit

## E4. notification-service

- [x] E4.1 notification-service: queue `notification.events` + DLQ nghe customer.created, order.confirmed/cancelled/shipped/delivered; template Thymeleaf tiếng Việt (layout chung, tiêu đề i18n `mail.<template>.subject`); `NotificationChannel` strategy (SMTP, Mailpit dev); `notification_log` idempotent theo (event_id, channel); retry 3 lần backoff rồi DLQ. Đã kiểm thật: email xác nhận/giao hàng/hủy vào Mailpit

## E5. Storefront (Next.js)

- [x] E5.1 Auth.js + Keycloak (BFF), proxy bảo vệ `/tai-khoan`, `/thanh-toan`, `/admin`; helper `authFetch` gọi gateway kèm token từ server (`src/lib/session.ts`)
- [x] E5.2 Trang chủ nối dữ liệu thật (danh mục, mới về); cache `use cache` 1 phút + `POST /api/revalidate` (làm sớm để demo C1)
- [x] E5.3 PLP: `/danh-muc/[slug]`, `/tim-kiem`, `/moi-ve`, `/sale`; lưới, thanh lọc dính, facet, sắp xếp, xem thêm, URL state; toggle mật độ lưới
- [x] E5.4 PDP: `/san-pham/[slug]`: gallery, chọn màu/size theo tồn kho (Suspense, không cache), khắc tên cho trang sức, accordion, sản phẩm liên quan; review là placeholder tới E7.1. Chưa có thanh dính đáy mobile
- [x] E5.5 Giỏ hàng: ngăn kéo desktop 440px (mở khi thêm hàng/bấm Giỏ hàng), trang `/gio-hang` cho mobile; khách vãng lai giữ giỏ ở trình duyệt và kiểm tra lại giá/tồn qua catalog khi tải trang; đăng nhập thì gộp vào giỏ server (`POST /api/cart/merge`) rồi dùng giỏ server qua Server Action; dòng hết hàng/vượt tồn khóa nút Thanh toán
- [x] E5.6 Thanh toán: `/thanh-toan` một trang (sổ địa chỉ hoặc nhập mới 2 cấp tỉnh/phường + lưu sổ, vận chuyển từ quote, COD/online, voucher, ghi chú), Idempotency-Key, `/thanh-toan/mock/{orderId}` (cổng giả ký HMAC), `/thanh-toan/thanh-cong/{code}`, `/tai-khoan/don-hang` + chi tiết + hủy đơn. Chưa kiểm tra giao diện mobile trên thiết bị thật
- [x] E5.7 Admin `/admin` (khung riêng UI-12, trang cửa hàng chuyển vào route group `(shop)`): tổng quan việc cần làm, đơn hàng theo tab + tìm + chi tiết + thao tác theo trạng thái (giao hàng chọn hãng, hủy có lý do, đổi trả), sản phẩm (danh sách, tạo, sửa thông tin chung, biến thể từng dòng, ảnh theo URL), tồn kho sửa tại chỗ. Chưa làm UI: giao hàng hàng loạt, voucher, khách hàng/nhân viên (API đã có), upload ảnh (chờ module media)
- [x] E5.8 SEO: canonical, `sitemap.xml` (danh mục + sản phẩm), `robots.txt`, JSON-LD Product + BreadcrumbList, prerender mọi sản phẩm/danh mục bằng `generateStaticParams`; slug sai trả 404 thật nhờ kiểm tra ở proxy (`seo-guard.ts`, vì Partial Prefetching gửi App Shell 200 trước). Lighthouse mobile (máy dev, `next start`): trang chủ 88/96/100/100, PDP 91/96/96/100, danh mục 88/91/100/100 (perf/a11y/best/SEO)

⏸ **CHECKPOINT C3 (MVP)**: mua hàng end-to-end trên trình duyệt từ trang chủ tới đơn thành công, admin xử lý đơn

## E6. Go-live (cần credential của chủ shop)

- [ ] E6.1 VNPay thật (sandbox trước) qua `PaymentGateway`. **Code xong** (B-24): `VnpayGateway` (2.1.0, HMAC-SHA512), IPN `GET /webhooks/payments/vnpay`, storefront chọn VNPay khi bật + trang `/thanh-toan/ket-qua/{code}`; đã thử đầu cuối với credential giả + IPN tự ký. **Còn:** tmnCode/hashSecret sandbox của chủ shop để thử với VNPay thật, rồi khai báo IPN URL public trong trang quản trị VNPay
- [ ] E6.2 MoMo thật qua `PaymentGateway`. **Code xong** (B-25): `MomoGateway` (API v2 `/v2/gateway/api/create`, HMAC-SHA256), IPN `POST /webhooks/payments/momo` (chữ ký trong body), test với MockRestServiceServer + chữ ký đối chiếu. **Còn:** partnerCode/accessKey/secretKey môi trường test của chủ shop để thử với MoMo thật
- [ ] E6.3 GHN/GHTK thật qua `ShippingCarrier`. **Đang chặn:** máy dev không truy cập được api.ghn.vn/api.ghtk.vn để đọc tài liệu hiện hành; API đổi theo địa giới 2 cấp (2025), GHN dùng mã phường riêng phải lấy bằng token. Cần token + tài liệu của chủ shop. Khi làm: mở rộng `ShippingCarrier.createTracking` nhận thông tin người nhận/hàng/COD, thêm địa chỉ lấy hàng của shop vào cấu hình
- [ ] E6.4 Keycloak theme Keycloakify tiếng Việt; Google login
- [x] E6.5 Realm as code (`deploy/keycloak`), CNPG, RabbitMQ, Valkey manifest, Argo CD app-of-apps, Sealed Secrets (B-26). Đã kiểm tra `kubectl kustomize` mọi thư mục + parse YAML; **chưa apply lên cluster thật** (chưa có VPS)
- [ ] E6.7 Storefront lên cluster: Dockerfile/Jib-tương đương cho Next.js standalone, chart hoặc manifest `storefront` (probe, non-root, read-only FS, NetworkPolicy), biến AUTH_* qua SealedSecret, thêm vào ApplicationSet
- [ ] E6.6 Load test checkout, thử restore backup, checklist go-live mục 17 thiết kế

## E7. Tăng trưởng

- [ ] E7.1 Review có ảnh + duyệt
- [ ] E7.2 Flash sale giữ hàng Redis
- [ ] E7.3 Giá vàng theo ngày (B-09)
- [ ] E7.4 Báo cáo doanh thu
