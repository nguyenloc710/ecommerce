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

## Không hardcode, dùng i18n
- Không hardcode giá trị nghiệp vụ hay cấu hình trong code: TTL giữ hàng, số lần retry, URL đối tác, kích thước lô, giới hạn upload... đưa vào `application.yml` và đọc qua `@ConfigurationProperties` (record, có prefix `ecommerce.<module>`). Giá trị cố định của nghiệp vụ (mã trạng thái, tên header, regex) đặt thành hằng có tên trong class `<Module>Constants`, không rải magic number/string.
- Không hardcode chuỗi hiển thị cho người dùng (message lỗi, tiêu đề email, nhãn trạng thái). Mọi chuỗi đi qua `MessageSource` với key trong `messages.properties` (vi) và `messages_en.properties` (en); nội dung email/Zalo dùng template theo `Locale`. Trong code chỉ xuất hiện key.
- Không hardcode secret, tài khoản, đường dẫn tuyệt đối; nhận qua biến môi trường.

## Tương tác qua interface
- Service tầng nghiệp vụ luôn là `interface + Impl`; controller, module khác và test chỉ phụ thuộc vào interface.
- Module khác gọi vào qua **interface public ở package gốc của module** (ví dụ `InventoryApi`), kèm record request/response riêng; impl và entity nằm ở package con. Không expose entity hay repository ra ngoài module.
- Gọi service khác hoặc đối tác (catalog, Keycloak Admin, VNPay, GHN) qua một interface adapter (`CatalogClient`, `PaymentGateway`) trong package `client`/`adapter`; nghiệp vụ không gọi `RestClient` trực tiếp, để test mock được và đổi nhà cung cấp không chạm service.
- Repository là interface Spring Data; không viết class truy vấn tùy ý ngoài repository.

## SOLID và design pattern: dùng khi có lợi thật
Mục tiêu là code dễ đổi ở đúng chỗ hay đổi (cổng thanh toán, hãng vận chuyển, kiểu giảm giá, kênh thông báo), không phải phủ pattern lên mọi thứ. Trước khi thêm một abstraction, trả lời được: *"điểm thay đổi nào nó bảo vệ?"*. Không trả lời được thì bỏ.

**SOLID áp vào dự án này**
- **S:** mỗi service class một trách nhiệm nghiệp vụ, mỗi method public một use case có tên nghiệp vụ. Checkout tách thành các bước có class riêng (tính giá, áp khuyến mãi, giữ hàng, tạo đơn) do một orchestrator gọi; không nhồi vào một `OrderServiceImpl` 1.000 dòng. Controller không nghiệp vụ, mapper không tính toán.
- **O:** chỗ chắc chắn sẽ mở rộng thì thiết kế để **thêm class, không sửa `switch`**: cổng thanh toán, hãng vận chuyển, kiểu giảm giá, kênh thông báo, cách tính giá (`FIXED` / `GOLD_RATE`). Spring inject `List<PaymentGateway>` rồi gom thành `Map<PaymentMethod, PaymentGateway>`; thêm MoMo = thêm một bean.
- **L:** mọi impl của cùng interface phải thay thế được nhau; không có impl ném `UnsupportedOperationException` hay cần caller `instanceof` để biết đang nói chuyện với ai.
- **I:** interface public của module chia theo nhu cầu người gọi (`StockReader` cho hiển thị, `StockReserver` cho checkout), không một `InventoryApi` 20 method bắt mọi module phụ thuộc hết.
- **D:** nghiệp vụ phụ thuộc interface; impl, adapter HTTP, repository nằm ở package con và được inject qua constructor. Ranh giới module/adapter của Spring Modulith đã là chỗ để áp dụng điều này, không cần thêm tầng.

**Pattern nên dùng ở đây (chỉ khi gặp đúng bài toán)**

| Bài toán | Pattern | Ghi chú |
|---|---|---|
| Nhiều nhà cung cấp cùng vai trò (thanh toán, vận chuyển, email/SMS/Zalo) | Strategy + adapter interface | Chọn impl theo enum qua `Map`, không `if/else` theo tên nhà cung cấp |
| Nhiều kiểu giảm giá, cách tính giá | Strategy | Mỗi kiểu một class, dễ test riêng |
| Vòng đời đơn hàng, thanh toán, vận đơn | State machine trong enum (`canTransitionTo(next)`) | Toàn bộ bảng chuyển trạng thái ở một chỗ; service gọi `assertTransition` trước khi ghi history |
| Lọc sản phẩm nhiều điều kiện tùy chọn | JPA `Specification` / Criteria | Thay cho chuỗi JPQL nối tay; vẫn giữ projection |
| Webhook nhiều đối tác cùng khung xử lý | Template Method (verify chữ ký → idempotency → parse → áp dụng) | Phần chung một chỗ, phần riêng ở lớp con |
| Dựng object nhiều tham số tùy chọn (request đối tác, event) | Builder (Lombok `@Builder` trên record/DTO) | Không viết builder tay |
| Tách ghi và đọc nặng (báo cáo, trang danh sách admin) | Query service riêng với projection | Không phải CQRS đầy đủ, không tách DB |

Outbox (Spring Modulith) và idempotent consumer là pattern bắt buộc đã có trong mục Event; không tự viết lại.

**Không làm**
- Không tạo interface cho thứ chỉ có một impl và không nằm ở ranh giới (mapper, DTO, helper nội bộ). Ngoại lệ: service nghiệp vụ và adapter ra ngoài (quy ước dự án).
- Không áp Abstract Factory, Visitor, Observer tự chế, Repository generic tự viết, "clean architecture" nhiều vòng tròn chồng lên Spring. Dùng cơ chế Spring có sẵn (event, `@ConfigurationProperties`, Spring Data) trước.
- Không trừu tượng hóa cho nhu cầu tương lai chưa có trong thiết kế (YAGNI). Khi nhu cầu đến, refactor có test bảo vệ.
- Method vượt Sonar Cognitive Complexity 15 hoặc hơn ~40 dòng: tách theo bước, đặt tên theo nghiệp vụ, đọc từ trên xuống.

## Audit, xóa mềm và lịch sử cập nhật
- **Audit cột:** mọi bảng nghiệp vụ có `created_at`, `created_by`, `updated_at`, `updated_by` (`TIMESTAMPTZ`, `by` = `sub` Keycloak hoặc `system` với job/consumer). Entity kế thừa `AuditableEntity` của service (`@MappedSuperclass`, `@CreatedBy/@LastModifiedBy...`); bật `@EnableJpaAuditing` với `AuditorAware` lấy user từ `CurrentUser`. Không set các cột này bằng tay từ request.
- **Xóa mềm tùy chức năng:** dữ liệu còn được tham chiếu hoặc cần tra cứu lại (sản phẩm, SKU, danh mục, thương hiệu, voucher, địa chỉ, tài khoản nhân viên) dùng xóa mềm bằng `deleted_at TIMESTAMPTZ NULL` (+ `deleted_by`), không `DELETE`. Mọi truy vấn đọc mặc định lọc `deleted_at IS NULL` (`@SQLRestriction` hoặc điều kiện trong query); unique index phải là partial index `WHERE deleted_at IS NULL` để cho phép tạo lại cùng slug/mã. Chỉ xóa cứng với dữ liệu tạm hoặc chưa được tham chiếu (giỏ hàng, item wishlist, reservation hết hạn, token). Ghi rõ lựa chọn trong hợp đồng của tính năng.
- **Lịch sử cập nhật:** thay đổi cần truy vết lưu thành bản ghi, không chỉ ghi đè: trạng thái đơn → `order_status_history` (from, to, reason, actor, at); giá/tồn kho/thông tin sản phẩm → Hibernate Envers (`@Audited`, bảng `*_aud`); thao tác nhân viên (sửa đơn, hoàn tiền, khóa khách, tạo voucher) → `admin_audit_log` (actor, action, target, before/after JSONB, trace_id). Luồng chuyển trạng thái phải kiểm tra trạng thái hiện tại có được phép chuyển không trước khi ghi lịch sử.
- **Kiểm tra trước khi cập nhật:** API cập nhật nhận `version` từ client và set vào entity để Optimistic Locking phát hiện bản ghi đã bị người khác sửa (trả `409 common.concurrent-modification`); không đọc rồi ghi đè mù. Danh sách chi tiết của các quy tắc audit nằm ở mục 12.3 của `docs/ke-hoach-thiet-ke.md`.

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
