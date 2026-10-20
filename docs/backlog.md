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
- [ ] E0.7 (nhỏ) JVM của `@SpringBootTest` thoát chậm 30s sau khi test xong (surefire phải kill fork); tìm thread non-daemon (RabbitMQ/Hikari) và tắt đúng cách
- [x] E0.6 Kiểm chứng hạ tầng local: `docker compose up` chạy, realm import được, lấy được token của `customer@example.com` bằng client `dev-cli` (thêm client này, bật password grant chỉ cho dev), 5 service khởi động với DB thật. Tiêu chí: `curl` qua gateway tới `/actuator/health` của từng service trả `UP`

## E1. catalog-service

- [x] E1.1 Module `product`: migration + entity `category` (cây, `attribute_template` JSONB), `brand`, `product`, `product_variant`, `product_image`; `AuditableEntity`, xóa mềm, Envers cho product/variant. API công khai: `GET /api/categories` (cây), `GET /api/products?category=&page=&size=&sort=` (projection card: tên, slug, giá, ảnh chính, màu), `GET /api/products/{slug}` (chi tiết + variant + ảnh). Tiêu chí: test service + `@DataJpaTest` Testcontainers, seed 12 sản phẩm mẫu bằng Flyway `R__seed_dev.sql` chỉ profile dev
- [x] E1.2 Module `product` admin: `POST/PUT/DELETE /api/admin/catalog/categories|brands|products|variants`, upload thứ tự ảnh, cập nhật nhận `version`. Tiêu chí: `@WebMvcTest` với `jwt()` role STAFF pass, CUSTOMER bị 403
- [x] E1.3 Module `inventory`: `inventory_item`, `reservation`; API nội bộ `POST /internal/skus/lookup`, `POST /internal/reservations` (idempotent theo orderId, TTL 15 phút), `POST /internal/reservations/{orderId}/commit|release`; job nhả hết hạn; admin `GET/PUT /api/admin/inventory`. Tiêu chí: test đồng thời 2 reservation trên 1 SKU không bán âm
- [x] E1.4 Module `search` (gộp vào module `product` dưới dạng `ProductSearchRepository`, không tách module riêng vì cùng bảng): cột `search_vector` + `unaccent`/`pg_trgm`, `GET /api/products?q=` tìm không dấu, facet đếm theo thuộc tính (`GET /api/products/facets?category=`). Tiêu chí: tìm "nhan bac" ra "Nhẫn bạc"
- [x] E1.5 Consumer `order.confirmed.v1` → commit reservation; `order.cancelled.v1` → release; idempotent qua `processed_event`. Tiêu chí: test gửi trùng event 2 lần chỉ trừ kho 1 lần

⏸ **CHECKPOINT C1**: demo catalog qua Swagger + storefront đọc được danh mục/sản phẩm thật

## E2. customer-service

- [ ] E2.1 Module `profile`: JIT tạo hồ sơ từ JWT lần đầu, `GET/PUT /api/customers/me`, event `customer.created.v1`
- [ ] E2.2 Module `address`: CRUD sổ địa chỉ, địa chỉ mặc định, dữ liệu tỉnh/quận/phường (bảng tĩnh seed)
- [ ] E2.3 Module `wishlist`: thêm/xóa/list theo SKU
- [ ] E2.4 Module `staffadmin`: adapter `KeycloakAdminGateway` (+ `Mock` cho test), `GET /api/admin/customers` (tìm, khóa/mở), `POST /api/admin/staff` tạo nhân viên vào group `staff`

## E3. order-service

- [ ] E3.1 Module `cart`: giỏ của khách đăng nhập (`cart`, `cart_item` có `engraving`), gộp giỏ khách vãng lai khi đăng nhập (`POST /api/cart/merge`), giá luôn tra lại từ catalog qua `CatalogClient`
- [ ] E3.2 Module `promotion`: `voucher` (giảm %/cố định, tối đa, điều kiện tối thiểu, hạn, số lượt), `POST /api/vouchers/validate`; admin CRUD
- [ ] E3.3 Module `shipping`: `ShippingCarrier` interface, `MockCarrier` (phí cố định theo B-07), tính phí theo địa chỉ, tạo vận đơn, webhook trạng thái
- [ ] E3.4 Module `checkout` + `orders`: `POST /api/orders` (Idempotency-Key) theo luồng 7.5 của thiết kế; `order_status_history`; state machine trong enum; `GET /api/orders/me`, `GET /api/orders/{code}`; event `order.confirmed/cancelled/shipped/delivered`
- [ ] E3.5 Module `payment`: `PaymentGateway` interface, `CodGateway`, `MockOnlineGateway` (trả URL giả + webhook giả để test), webhook idempotent theo `provider_txn_id`, kiểm tra chữ ký qua interface
- [ ] E3.6 Admin đơn hàng: `GET /api/admin/orders` (lọc theo tab trạng thái, tìm), chuyển trạng thái có kiểm tra, `admin_audit_log`, tạo vận đơn hàng loạt

⏸ **CHECKPOINT C2**: đặt được đơn COD end-to-end bằng Swagger/curl, kho trừ đúng, email xác nhận vào Mailpit

## E4. notification-service

- [ ] E4.1 Consume `order.confirmed/shipped/delivered`, `customer.created`; template Thymeleaf tiếng Việt; gửi qua SMTP (Mailpit dev); `notification_log` idempotent; retry + DLQ

## E5. Storefront (Next.js)

- [ ] E5.1 Auth.js + Keycloak (BFF), middleware bảo vệ `/account`, `/admin`; helper gọi gateway kèm token từ server
- [x] E5.2 Trang chủ nối dữ liệu thật (danh mục, mới về); cache `use cache` 1 phút + `POST /api/revalidate` (làm sớm để demo C1)
- [ ] E5.3 PLP: lưới, thanh lọc dính, facet, sắp xếp, xem thêm, URL state; mobile toggle 1/2 cột
- [ ] E5.4 PDP: gallery, chọn màu/size, tồn kho, khắc tên, accordion, hoàn thiện bộ, review; mobile thanh dính đáy
- [ ] E5.5 Giỏ hàng: Zustand cho khách vãng lai, ngăn kéo desktop, trang mobile, gộp giỏ khi đăng nhập
- [ ] E5.6 Thanh toán: form địa chỉ 3 cấp, vận chuyển, thanh toán, voucher, đặt hàng, trang thành công, trang đơn của tôi
- [ ] E5.7 Admin `/admin`: đăng nhập STAFF, danh sách đơn theo tab, chi tiết đơn, đổi trạng thái; sản phẩm CRUD cơ bản; tồn kho
- [ ] E5.8 SEO: metadata, sitemap, robots, structured data Product; Lighthouse mobile ≥ 85

⏸ **CHECKPOINT C3 (MVP)**: mua hàng end-to-end trên trình duyệt từ trang chủ tới đơn thành công, admin xử lý đơn

## E6. Go-live (cần credential của chủ shop)

- [ ] E6.1 VNPay thật (sandbox trước) qua `PaymentGateway`
- [ ] E6.2 MoMo thật qua `PaymentGateway`
- [ ] E6.3 GHN/GHTK thật qua `ShippingCarrier`
- [ ] E6.4 Keycloak theme Keycloakify tiếng Việt; Google login
- [ ] E6.5 Realm as code (`deploy/keycloak`), CNPG, RabbitMQ, Valkey manifest, Argo CD app-of-apps, Sealed Secrets
- [ ] E6.6 Load test checkout, thử restore backup, checklist go-live mục 17 thiết kế

## E7. Tăng trưởng

- [ ] E7.1 Review có ảnh + duyệt
- [ ] E7.2 Flash sale giữ hàng Redis
- [ ] E7.3 Giá vàng theo ngày (B-09)
- [ ] E7.4 Báo cáo doanh thu
