---
name: ecom-review
description: Review code backend của dự án ecommerce theo bộ rule riêng (ranh giới module Spring Modulith, exception/message key, phân quyền Keycloak, idempotency, snapshot đơn hàng, migration bất biến, outbox/event, log không lộ PII). Dùng khi người dùng nói "review code", "check rule", "rà soát", "/ecom-review", hoặc ở bước tự kiểm tra cuối của /ecom-feature. Chỉ liệt kê vi phạm và đề xuất, không tự sửa khi chưa được đồng ý.
argument-hint: "[đường dẫn | tên service | để trống = diff hiện tại]"
allowed-tools: Read, Grep, Glob, Bash(git diff *), Bash(git status *), Bash(./mvnw *), Bash(.\\mvnw.cmd *)
---

# /ecom-review: review theo rule của dự án

Phạm vi: $ARGUMENTS (để trống thì review `git diff` của working tree so với `main`, gồm cả file mới chưa track).

Đi theo đúng thứ tự dưới đây, mỗi mục đọc code thật rồi mới kết luận. Với mỗi vi phạm ghi: **file:dòng**, kịch bản lỗi cụ thể ("2 khách mua cùng lúc → bán âm kho"), đề xuất sửa, và mức **phải sửa** / **nên sửa**. Không tự sửa file.

## 1. Ranh giới module và service
- Class ở module A import type thuộc package con (không phải package gốc) của module B? → phải sửa.
- Inject repository/entity của module khác? → phải sửa.
- Gọi DB hoặc đọc bảng thuộc service khác? → phải sửa (chỉ REST `/internal` hoặc event).
- Package mới ở gốc service không phải module nghiệp vụ (config, util)? → nên sửa, dời về package gốc.
- `ModularityTests` có bị sửa để nới lỏng không?

## 2. Hợp đồng API và phân quyền
- Endpoint đúng nhóm: khách `/api/**`, nhân viên `/api/admin/**`, service-service `/internal/**`. Endpoint `/internal` có đi qua gateway không (không được).
- `customerId`/`userId` lấy từ `CurrentUser`, không từ request. Có kiểm tra quyền sở hữu tài nguyên trong service không (khách A xem đơn khách B).
- Webhook: kiểm tra chữ ký trước khi xử lý; idempotent theo `provider_txn_id`.
- Tạo đơn có nhận `Idempotency-Key` và chống tạo trùng không.

## 3. Lỗi và message
- Báo lỗi bằng `ApiException` con với message key; không `return null`, không `ResponseEntity` lỗi từ service, không try/catch nuốt lỗi.
- Key mới có trong cả `messages.properties` và `messages_en.properties`; placeholder `{0}` khớp số args.
- Có log stack trace ở service (trùng với `GlobalExceptionHandler`)?

## 4. Dữ liệu và transaction
- Migration: file đã commit có bị sửa không; thay đổi có tương thích ngược không; cột lọc/join có index; bảng cập nhật đồng thời có `@Version`.
- Entity khớp SQL (kiểu, nullable, độ dài).
- Tiền `BigDecimal`, id `UUID`, thời gian `Instant`.
- Luồng ghi: validate hết rồi mới ghi; nhiều bảng trong một `@Transactional`; không gọi `@Transactional` nội bộ cùng class.
- Query trong vòng lặp, `SELECT` nguyên entity khi chỉ cần vài cột, `IN` không chia lô.
- Đơn hàng: giá lấy từ catalog, không tin client; snapshot tên/giá/biến thể vào `order_item`.
- Tồn kho: `available = on_hand - reserved`, reservation có `expires_at`, optimistic lock, không khóa bi quan.

## 5. Event
- Publish bằng `ApplicationEventPublisher` trong `@Transactional` (không `RabbitTemplate` trực tiếp).
- Record event trong `common-events` + routing key trong `EventRoutes`; có đổi nghĩa field cũ không (phải tạo `.v2`).
- Consumer: ghi `processed_event` trong cùng transaction với xử lý; exception không bị nuốt (để retry/DLQ).

## 6. Bảo mật và log
- Log có token, header `Authorization`, mật khẩu, email/số điện thoại chưa che, body webhook thanh toán nguyên vẹn?
- Secret thật trong code, yml, test, docker-compose?
- Endpoint mới có vô tình được `permitAll` qua `ecommerce.security.public-*`?

## 7. Test và sạch code
- Mỗi nhánh lỗi một test; verify không ghi khi lỗi; mapper thật, không mock mapper.
- Query phức tạp test bằng Testcontainers PostgreSQL, không H2.
- Magic number/string, Optional làm field/param, `Date`/`double` cho tiền/thời gian, comment tiếng Anh lẫn tiếng Việt.

## Kết quả
Trả về theo mẫu:

```
### Phải sửa
1. <file:dòng> — <vi phạm> — <kịch bản lỗi> — <đề xuất>
### Nên sửa
...
### Đã kiểm tra, không thấy vấn đề
- <mục 1..7 nào đã xem>
```

Nếu được yêu cầu, chạy `./mvnw -pl <service> -am test` để xác nhận test hiện tại pass trước khi kết luận.
