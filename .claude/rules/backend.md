---
paths:
  - "backend/**"
---

# Quy tắc viết code backend

Áp dụng cùng skill global `java-spring-boot-senior` (phân tầng, projection, batch IN, transaction, test). Dưới đây chỉ là phần đặc thù của dự án này.

## Ranh giới module (Spring Modulith)
- Code của module nằm trong package của nó; chỉ type ở **package gốc của module** (ví dụ `vn.ecommerce.catalog.inventory.InventoryApi`) được module khác gọi. Repository, entity, service impl đặt ở package con (`internal`, `domain`, `web`...) để không lộ ra ngoài.
- Module khác nhau trong cùng service giao tiếp bằng gọi API public của module hoặc `ApplicationEventPublisher`. Không inject repository của module khác.
- Service khác nhau **không bao giờ** đọc chung database; cần dữ liệu thì gọi REST (`/internal/**`) hoặc nhận qua event.
- Thêm module mới = tạo package con ở gốc + `package-info.java` có `@ApplicationModule`. Code hạ tầng (config, security) đặt ở package gốc của service, không tạo package riêng ở gốc.

## Lỗi và message
- Service ném `BadRequestException` / `NotFoundException` / `ConflictException` / `ForbiddenException(messageKey, args...)`. Không trả `null`, không trả `ResponseEntity` để báo lỗi, không `try/catch` trong controller.
- Message key dạng `<module>.<ten-loi>` (ví dụ `inventory.out-of-stock`), khai báo trong `messages.properties` (tiếng Việt) và `messages_en.properties`. Hằng key đặt trong class `<Module>ErrorKeys`, Javadoc ghi rõ `{0}`, `{1}` là gì.
- Không log stack trace ở service; `GlobalExceptionHandler` đã làm việc đó.

## Người dùng và phân quyền
- Lấy người dùng từ `CurrentUser` (`requireId()` = `sub` của Keycloak). Không nhận `customerId` từ request body hay query.
- Endpoint khách: `/api/...`; nhân viên: `/api/admin/...`; service gọi nhau: `/internal/...`. Trong service vẫn kiểm tra quyền sở hữu (khách chỉ xem đơn của mình), không chỉ dựa vào gateway.
- Webhook (`/webhooks/**`) không có JWT: kiểm tra chữ ký và idempotent theo mã giao dịch của đối tác.

## Dữ liệu
- Schema chỉ đổi qua Flyway (`src/main/resources/db/migration/V<n>__mo_ta.sql`), `ddl-auto: validate`. File đã commit là bất biến (hook sẽ chặn); thay đổi phải tương thích ngược (expand → migrate → contract).
- Id dùng `UUID`; tiền dùng `BigDecimal` cột `NUMERIC(19,2)`; thời gian `Instant`/`TIMESTAMPTZ`.
- Entity có `@Version` cho mọi bảng bị cập nhật đồng thời (tồn kho, đơn hàng, giỏ). Không khóa bi quan.
- Đơn hàng lưu **snapshot** tên, biến thể, giá tại thời điểm đặt. Không bao giờ tin giá client gửi lên; luôn tra lại từ catalog.
- Tạo đơn và webhook thanh toán phải **idempotent** (`Idempotency-Key` / `provider_txn_id`).

## Event
- Hợp đồng event là record trong `common-events`, routing key trong `EventRoutes` theo mẫu `<domain>.<event>.v<n>`. Chỉ thêm field; đổi nghĩa thì tạo `.v2`.
- Publish bằng `ApplicationEventPublisher` **bên trong** `@Transactional`; Spring Modulith ghi vào `event_publication` cùng transaction rồi mới đẩy sang RabbitMQ. Không gọi `RabbitTemplate` trực tiếp từ nghiệp vụ.
- Consumer idempotent bằng bảng `processed_event (event_id, consumer)`; lỗi không phục hồi được thì để đi DLQ, không nuốt exception.

## Logging
- `@Slf4j`, tham số `{}`. Không log token, header `Authorization`, mật khẩu, nội dung thẻ. Email/số điện thoại/địa chỉ phải che (`n***@gmail.com`).
- Gắn `orderId`, `paymentProvider` vào MDC ở các luồng checkout/webhook để tra theo mã đơn.

## Test
- Service: Mockito, mapper thật, mỗi nhánh lỗi một test, verify không ghi gì khi lỗi.
- Query phức tạp: `@DataJpaTest` + Testcontainers PostgreSQL (không H2).
- Mọi service có `ModularityTests`; thêm module hay đổi phụ thuộc giữa module thì chạy lại.
- Test security dùng `spring-security-test` (`jwt()` post-processor với claim `roles`).
