# Tình trạng checklist go-live

Đối chiếu từng mục của thiết kế mục 17 với code hiện tại (cập nhật 23/10/2026). Ký hiệu:

- ✅ có bằng chứng: test tự động hoặc manifest đã kiểm tra.
- 🟡 code/manifest sẵn nhưng phải chạy trên staging mới tính là đạt.
- ❌ chưa có, cần làm hoặc cần quyết định.

| # | Mục | Tình trạng | Bằng chứng / việc còn lại |
|---|---|---|---|
| 1 | Probes, graceful shutdown, PDB (prod), NetworkPolicy | ✅ | Chart `deploy/charts/spring-service`: startup/liveness/readiness, `preStop` + graceful shutdown, `pdb.yaml`, `networkpolicy.yaml`; namespace default-deny (`deploy/platform/default-deny.yaml`) |
| 2 | Spring Modulith verify pass | ✅ | `ModularityTests` ở cả 4 service servlet, chạy trong `./mvnw verify` |
| 3 | Keycloak: admin console kín, brute force, OTP cho STAFF/ADMIN, SMTP | 🟡 / ❌ | Ingress chỉ mở `/realms`, `/resources` (không có `/admin`); realm bật `bruteForceProtected`; có cấu hình SMTP (cần credential Brevo). **Thiếu: bắt buộc OTP cho STAFF/ADMIN** (flow đăng nhập có điều kiện theo role) |
| 4 | Realm quản lý bằng code, staging = prod | ✅ | Realm as code (B-26), prod sinh từ realm dev bằng `deploy/keycloak/base/realm/build-realm.mjs` |
| 5 | Token hết hạn, refresh bị thu hồi, đăng xuất mọi thiết bị, khóa user | 🟡 | Proxy storefront làm mới token, refresh token xoay vòng. Phải thử tay trên staging: khóa user trong Keycloak → lần refresh tiếp theo bị đá ra |
| 6 | Từ chối token sai `iss`/`aud`; `/internal/**` từ chối token khách | ✅ | `JwtValidationTest` (common-security: sai iss, sai aud, hết hạn đều bị từ chối, token ký thật qua JWKS); `InventoryControllerTest.internalLookup_requiresInternalRole` |
| 7 | Backup PostgreSQL (gồm `keycloak_db`) lên R2, đã restore thử | 🟡 | `ScheduledBackup` 02:00 + WAL archive (`deploy/data/base/postgres-backup.yaml`); manifest diễn tập `deploy/data/restore/` (cluster `pg-restore` + NetworkPolicy, đã `kubectl kustomize`); quy trình ở `docs/runbook.md` mục 1. **Cần bucket R2 để chạy thật** |
| 8 | Webhook thanh toán: trùng, sai chữ ký, thanh toán trễ sau khi đơn hết hạn | ✅ / 🟡 | `PaymentServiceImplTest` (sai chữ ký, trùng giao dịch, sai số tiền, thất bại), `VnpayIpnHandlerTest`, `MomoGatewayTest`, `OrderServiceImplTest` (tiền về sau khi đơn đã hủy: giữ CANCELLED, ghi log cảnh báo hoàn tiền tay, B-21). **Nên thêm:** hiện đơn "đã hủy nhưng đã nhận tiền" ở trang admin, không chỉ trong log. Chạy thật với sandbox VNPay/MoMo (E6.1/E6.2) |
| 9 | Reservation hết hạn được nhả; không bán âm kho khi 2 khách mua cùng lúc | ✅ | `InventoryReservationDbTest` (tranh chấp thật trên PostgreSQL), `ReservationServiceImplTest` (hết hạn, idempotent) |
| 10 | Load test checkout và đăng nhập đạt gấp 3 lần tải dự kiến | 🟡 | Script k6 `loadtest/checkout.js` + `loadtest/README.md` (browse/login/checkout, ngưỡng p95, cách tính tốc độ). Cần staging + số đơn/ngày dự kiến (mục 18) |
| 11 | Alert đến đúng người; runbook node chết, DB failover, Keycloak không lên | 🟡 / ❌ | Runbook: `docs/runbook.md` (khôi phục backup, node chết, failover, Keycloak, RabbitMQ). **Thiếu: stack giám sát** (Grafana/Loki/Alloy hoặc Grafana Cloud) và bảng liên hệ trong runbook |
| 12 | Log JSON có `trace.id`, `user.id`; lỗi trả `traceId`; tra request xuyên service | 🟡 / ❌ | Log ECS JSON khi chạy trên K8s, `traceId` trong `ErrorResponse` và header `X-Trace-Id`, observation RabbitMQ bật. Export trace mặc định tắt (`TRACING_EXPORT_ENABLED`) vì **chưa có collector** (Alloy) trong `deploy/platform` |
| 13 | Log không chứa mật khẩu, token, `Authorization`, PII chưa che | 🟡 | Đã rà câu lệnh log trong code: email che bằng `mask()`, không log token/header. Cần kiểm tra lại trên log thật ở staging |
| 14 | Alert từ log (ERROR tăng, webhook sai chữ ký, đăng nhập thất bại) đã bắn thử | ❌ | Phụ thuộc mục 11–12 (stack log + alert) |
| 15 | Audit thao tác nhân viên và lịch sử giá | ✅ | `admin_audit_log` qua `AdminAuditRecorder` (đơn, voucher, đánh giá, flash sale, giá vàng), Envers `*_aud` cho sản phẩm/variant (giá vàng ghi lại giá variant nên có lịch sử) |
| 16 | VPS chỉ nhận traffic từ Cloudflare; SSH chỉ dùng key | ❌ | `infra/k3s` còn trống; làm khi có VPS (E6.6) |
| 17 | SEO: sitemap, robots, structured data Product, ảnh có alt | ✅ | `sitemap.xml`, `robots.txt` (Next), JSON-LD Product + aggregateRating trên trang sản phẩm, `next/image` có `alt` |

## Việc còn lại theo nhóm

**Làm được ngay, không tốn tiền:**
- Bắt buộc OTP cho tài khoản STAFF/ADMIN trong realm as code (mục 3).
- Hiện đơn "đã hủy nhưng đã nhận tiền" ở trang admin để hoàn tiền (mục 8).

**Cần quyết định hoặc tài khoản (P-02):**
- Stack giám sát: tự host Grafana + Loki + Alloy trên cụm (tốn RAM node) hay Grafana Cloud free (cần tài khoản). Ảnh hưởng mục 11, 12, 14.
- VPS + domain + Cloudflare + R2 + Brevo: mục 3 (SMTP), 7, 10, 16.
- Số đơn/ngày dự kiến để đặt tốc độ load test (mục 10).

## Kết quả chạy trên staging

| Ngày | Mục | Kết quả | Ghi chú |
|---|---|---|---|
| | Diễn tập khôi phục (mục 7) | | |
| | Load test (mục 10) | | |
