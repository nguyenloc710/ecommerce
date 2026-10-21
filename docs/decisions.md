# Quyết định đã chốt (decisions)

Nguồn mặc định khi code tự động. Gặp điểm mơ hồ không có ở đây: chọn phương án đơn giản nhất khớp `ke-hoach-thiet-ke.md`, ghi thêm một dòng vào bảng tương ứng kèm lý do, rồi đi tiếp. Không hỏi.

## Quy trình

| # | Quyết định | Lý do / ghi chú |
|---|---|---|
| P-01 | Mỗi task trong `backlog.md` làm trên nhánh `feat|fix|chore/<service>-<topic>`, chạy `./mvnw -pl <service> -am verify` (BE) hoặc `npm run lint && npm run build` (FE) xanh rồi **fast-forward vào `main` và push** | Chủ shop đã chọn merge thẳng vào main nhiều lần; CI backend vẫn chạy trên main để bắt lỗi môi trường |
| P-02 | Dừng và hỏi chỉ khi: cần credential/tài khoản của chủ shop (VNPay, MoMo, GHN, GHTK, Google OAuth, VPS, Cloudflare), thao tác xóa dữ liệu hoặc tốn tiền, CI đỏ 2 lần liên tiếp không tự sửa được, hoặc tới **checkpoint** ghi trong backlog | |
| P-03 | Tích hợp đối tác chưa có credential: viết adapter interface + impl `Mock*` dùng ở dev/staging, impl thật để sẵn với config rỗng, bật qua profile | Không chặn tiến độ, đổi nhà cung cấp không chạm nghiệp vụ |
| P-04 | Ngôn ngữ: code/identifier tiếng Anh; comment, log, message, tài liệu, commit tiếng Việt | Quy ước đã có trong CLAUDE.md |
| P-05 | Test tích hợp dùng Testcontainers (PostgreSQL, RabbitMQ, Keycloak). Docker đang chạy trên máy dev | Image test `postgres:16-alpine` (có sẵn trên máy, Docker Hub tải chậm); prod 17. Đổi về 17 khi mạng ổn |
| P-07 | Port local: service 8080/8091–8094, Valkey 6382, Mailpit 1026/8026; image từ `public.ecr.aws` và `ghcr.io` | Máy dev đang chạy dự án khác chiếm 808x, 6379–6381, 8025; Docker Hub tải lỗi |
| P-08 | Client Keycloak `dev-cli` (public, password grant) chỉ tồn tại ở realm local để lấy token bằng curl/Postman | Không đưa vào realm staging/prod |
| P-06 | API trả **mã** cho nhãn/trạng thái (`NEW`, `SOLD_OUT`, `CONFIRMED`...), FE dịch sang tiếng Việt; chuỗi hiển thị chỉ có ở message lỗi (đã i18n) | Backend không trộn ngôn ngữ vào dữ liệu |

## Giao diện (đã chốt ngày 20/10/2026 trên canvas mẫu)

| # | Quyết định | Giá trị |
|---|---|---|
| UI-01 | Phong cách | Editorial tối giản kiểu Zara/COS/Mejuri: nền trắng, chữ gần đen, không màu nhấn, không bo góc, không bóng, không badge màu |
| UI-02 | Màu | `ink #111111`, `paper #ffffff`, `bone #f4f1ec`, `stone #e7e2da`, `sand #d9d2c7`, `taupe #cfc6ba`, `muted #6b6b6b`, `line #e5e5e5`, `sale #a3231b`, `gold #8a6d33`, `ok #1d5c33` |
| UI-03 | Font | Tiêu đề **Cormorant Garamond** 400/500/600 (có subset tiếng Việt); nội dung **Be Vietnam Pro** 300/400/500 |
| UI-04 | Chữ | Body 14px/1.55; eyebrow 11px, viết hoa, giãn `.16em`; nav 12px giãn `.12em`; H1 trang chủ 84px, H1 trang trong 56px, H1 PDP 38px; mobile giảm ~45% |
| UI-05 | Nút | Cao 52px (44px nút phụ), nền đen chữ trắng viết hoa 12px giãn `.14em`, hover đảo màu; không bo góc |
| UI-06 | Header | 80px desktop / 56px mobile; menu trái, **logo serif giữa** (chữ hoa giãn `.22em`), tiện ích phải; thanh thông báo đen 11px phía trên |
| UI-07 | Lưới sản phẩm | Ảnh 3:4, không viền; tên 13px, giá 13px, chấm màu 9px; nhãn "Mới/Bán chạy/-20%/Hết hàng" là chữ nhỏ góc trên trái ảnh; desktop 4 cột (toggle 2/4), mobile 2 cột (toggle 1/2) |
| UI-08 | PLP | Thanh lọc dính trên đầu (Bộ lọc +, Chất liệu, Size, Màu, Giá, Sắp xếp, mật độ); chip lọc đang áp dụng; "Đang hiển thị n/N" + Xem thêm, không phân trang số |
| UI-09 | PDP | Desktop: gallery 2 cột xếp dọc bên trái 58%, khung mua hàng dính bên phải 42%; mobile: ảnh tràn ngang + thanh "Thêm vào giỏ · giá" dính đáy. Accordion: Chi tiết / Chất liệu & bảo quản / Giao hàng & đổi trả |
| UI-10 | Giỏ hàng | Desktop: ngăn kéo trượt từ phải 440px; mobile: trang riêng. Tạm tính + nút Thanh toán, ghi chú phí ship tính ở bước sau |
| UI-11 | Thanh toán | Một trang: trái form (liên hệ, địa chỉ 3 cấp, vận chuyển, thanh toán dạng dòng kẻ), phải tóm tắt nền `bone`; mobile tóm tắt thu gọn ở đầu + nút Đặt hàng dính đáy |
| UI-12 | Admin | Phong cách Shopify admin: nền `#f6f6f7`, sidebar sáng, thẻ bo 12px, bảng có chọn nhiều |
| UI-13 | Khoảng cách | Gutter 48px desktop / 16px mobile; section cách nhau 96px desktop / 32px mobile; gap lưới 16–24px |
| UI-14 | Ảnh | `next/image`, placeholder tông `stone/sand/taupe` khi chưa có ảnh; CDN R2 |
| UI-15 | Tiền | `Intl.NumberFormat("vi-VN")` + "₫", ví dụ `1.290.000₫` |
| UI-16 | Tên shop | Chưa có, dùng placeholder "Tên shop" trong code qua biến `NEXT_PUBLIC_SHOP_NAME` |
| UI-17 | Ảnh mẫu | Ảnh Pexels (miễn phí) để tĩnh trong `frontend/storefront/public/img/seed/`, URL `/img/seed/...` lưu trong `product_image` của seed dev; nguồn ghi ở `CREDITS.md`. Ảnh thật sẽ qua module media (R2) với URL tuyệt đối, FE khai báo `images.remotePatterns` khi đó |
| UI-18 | Đăng nhập storefront | Auth.js v5 (`next-auth@5.0.0-beta.32`, ghim bản chính xác vì v5 chưa stable), trang `/dang-nhap` (một nút, đăng ký nằm ở Keycloak), `/tai-khoan`, `/admin`. `src/proxy.ts` chặn `/tai-khoan`, `/thanh-toan`, `/admin` (admin cần STAFF/ADMIN, không đủ quyền về `/`), làm mới token trước hạn 60s và chép cookie session mới vào request hiện tại vì refresh token xoay vòng (`revokeRefreshToken`) nên Server Component không được tự làm mới lần hai. Đăng xuất = xóa session + RP-initiated logout Keycloak |
| UI-19 | Giỏ hàng trên storefront | Một store Zustand hai chế độ: `guest` (lưu localStorage) và `server` (bản sao giỏ order-service, không lưu localStorage để không gộp hai lần). Sau khi gộp thành công giỏ trình duyệt bị xóa. Mọi thao tác ở chế độ server cập nhật lạc quan rồi gọi Server Action, lỗi thì tải lại giỏ server. Khách vãng lai chỉ biết số còn lại khi tồn thấp (`remaining` của catalog), số lượng chính xác kiểm tra lại ở checkout |
| UI-20 | Trang thanh toán | Form React Hook Form + Zod (`checkout-shared.ts`, Server Action kiểm tra lại). Báo giá gọi lại (debounce 300ms) khi đổi giỏ/địa chỉ đã lưu/hãng/voucher; voucher áp bằng `/api/vouchers/validate` để có message đã dịch. Idempotency-Key sinh khi bấm Đặt hàng và **giữ lại nếu lỗi mạng** (bấm lại nhận đơn cũ), lỗi nghiệp vụ thì sinh key mới (bổ sung B-17). Đơn online chờ thanh toán chuyển tới `/thanh-toan/mock/{orderId}?ma={code}` (storefront thêm `ma` để tra đơn qua API của khách); Server Action ký HMAC bằng `PAYMENT_MOCK_SECRET` và gọi webhook, không có biến này thì cổng giả tắt. URL chi tiết đơn dùng mã đơn `/tai-khoan/don-hang/{code}` (khớp link trong email) |

## Nghiệp vụ

| # | Quyết định | Giá trị |
|---|---|---|
| B-01 | Tiền tệ | VND, `BigDecimal` `NUMERIC(19,2)`, không bán quốc tế ở MVP |
| B-02 | Mã đơn | `ORD-yymmdd-NNNN` (NNNN tăng theo ngày), hiển thị admin dạng `#yymmdd-NNNN` |
| B-03 | Giữ hàng | TTL 15 phút, job nhả mỗi phút |
| B-04 | Trạng thái đơn | `PENDING → AWAITING_PAYMENT → CONFIRMED → PACKING → SHIPPING → DELIVERED → COMPLETED`; nhánh `CANCELLED`, `RETURN_REQUESTED → RETURNED → REFUNDED` |
| B-05 | Phân trang API | `page` 0-based, `size` mặc định 24 (PLP) / 20 (admin), tối đa 100 |
| B-06 | Slug | Sinh từ tên, bỏ dấu tiếng Việt, duy nhất trong bảng (partial unique `deleted_at IS NULL`) |
| B-07 | Freeship | Đơn từ 500.000₫; phí mặc định GHN 25.000₫, GHTK 18.000₫ khi chưa có API thật |
| B-08 | Khắc tên | Tối đa 10 ký tự, thuộc tính của dòng giỏ/đơn, không đổi trả nếu đã khắc |
| B-09 | Giá vàng | `pricing_type = GOLD_RATE`: giá = trọng lượng × giá vàng ngày (bảng `gold_rate`, admin nhập tay) + phí gia công; chốt giá lúc đặt |
| B-10 | Review | Chỉ khách có đơn `DELIVERED`/`COMPLETED` chứa SKU đó; duyệt tay trước khi hiện |
| B-11 | Admin role | `STAFF` không xem báo cáo doanh thu, không sửa voucher, không quản lý nhân viên; `ADMIN` toàn quyền |
| B-12 | Thông báo | Email qua SMTP (Mailpit local, Brevo prod); Zalo ZNS/SMS giai đoạn 3 |
| B-13 | Đơn vị hành chính | 2 cấp tỉnh/phường theo sắp xếp 2025 (34 tỉnh, 3.321 phường). Mã theo chuẩn GSO zero-pad: tỉnh 2 chữ số (`01`), phường 5 chữ số (`00004`). Địa chỉ lưu kèm snapshot tên tỉnh/phường. Nguồn provinces.open-api.vn v2, nạp bằng Flyway repeatable (chỉ thêm/cập nhật, không xóa) |
| B-14 | Khóa khách hàng | Gọi Keycloak Admin API trong cùng transaction DB (Keycloak lỗi → rollback, trả 502 `staffadmin.keycloak-unavailable`); khóa = `enabled=false` + logout mọi phiên + `customer_profile.status=LOCKED` + `admin_audit_log`; không tự khóa chính mình |
| B-15 | Địa chỉ khi checkout | order-service không nhận địa chỉ từ client mà gọi `GET /internal/customers/{id}/addresses/{addressId}` (role INTERNAL) rồi snapshot JSONB vào đơn; khách vãng lai nhập địa chỉ trực tiếp, validate tỉnh/phường qua `/api/divisions` |
| B-16 | Giỏ hàng | Khách đăng nhập: một giỏ server/khách, dòng = (sku, nội dung khắc), không lưu giá (tra catalog mỗi lần xem). Khách vãng lai: giỏ ở trình duyệt, `POST /api/cart/merge` cộng dồn khi đăng nhập. Checkout gửi `lines` tường minh (không đọc giỏ server), đặt xong dọn các SKU đã đặt khỏi giỏ. Checkout yêu cầu đăng nhập |
| B-17 | Idempotency-Key | Bắt buộc trên `POST /api/orders`, FE sinh UUID mỗi lần bấm Đặt hàng; unique (customer_id, key); gọi lại trả đơn cũ với 200 (lần đầu 201) |
| B-18 | Thanh toán online MVP | `MOCK_ONLINE`: payUrl trỏ trang giả trên storefront, webhook `/webhooks/payments/mock` ký HMAC-SHA256(secret, body) header `X-Signature`; VNPay/MoMo thêm bằng bean `PaymentGateway` mới ở E6. Webhook lệch số tiền → payment FAILED + hủy đơn |
| B-19 | Vận chuyển MVP | `MockCarrier` sinh từ cấu hình `ecommerce.shipping.carriers` (GHN 25k, GHTK 18k, freeship ≥ 500k), mã vận đơn `MOCK-<HÃNG>-xxxxxxxx`, webhook `/webhooks/shipping/mock` (HMAC) đổi trạng thái; bean hãng thật cùng mã sẽ thay hãng giả |
| B-20 | Hết hạn thanh toán | Đơn AWAITING_PAYMENT quá `ecommerce.orders.payment-timeout` (15 phút = TTL giữ hàng B-03) bị job hủy mỗi phút → event order.cancelled → catalog nhả hàng |
| B-21 | Tiền về sau khi hủy | Webhook thành công cho đơn đã CANCELLED: payment vẫn ghi SUCCEEDED, đơn giữ CANCELLED, log WARN để hoàn tiền tay (màn hình đối soát ở giai đoạn sau) |
| B-22 | Hủy đơn đã chốt kho | COD xác nhận ngay nên reservation COMMITTED (on_hand đã trừ). Hủy trước khi giao (đến PACKING) → catalog cộng lại on_hand và đánh dấu RELEASED. Hoàn hàng sau giao (RETURNED) nhập kho tay ở giai đoạn sau |
| B-23 | Email thông báo | Một event gửi tối đa một lần mỗi kênh (`notification_log` unique (event_id, channel)); gửi xong mà ghi log lỗi có thể gửi trùng khi retry, chấp nhận với email. Event thiếu email ghi SKIPPED. Ngôn ngữ email theo `ecommerce.notification.default-locale` (vi) |
