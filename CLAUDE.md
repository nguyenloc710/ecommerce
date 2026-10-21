# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Website bán thời trang, phụ kiện, trang sức: 5 microservice Spring Boot + Keycloak + Next.js, deploy lên k3s.
**Nguồn sự thật về thiết kế: `docs/ke-hoach-thiet-ke.md`** (bản 3.1). Khi code lệch thiết kế, cập nhật tài liệu cùng lúc.
Comment, log, message lỗi và tài liệu viết bằng tiếng Việt.

## Quy trình làm việc

Cấu hình dự án nằm trong `.claude/` (commit, cả team dùng chung); cấu hình cá nhân để ở `.claude/settings.local.json` hoặc `CLAUDE.local.md` (đã gitignore).

| Tình huống | Làm theo |
|---|---|
| Tính năng backend vừa/lớn | Plan mode → `/ecom-feature` → `/ecom-review` → `/code-review` → commit, PR theo template |
| Sửa nhỏ | Sửa trực tiếp → `/ecom-review` → commit, PR |
| Review PR người khác | `/ecom-review <đường dẫn hoặc service>` rồi `/code-review` |
| Lệch thiết kế | Cập nhật `docs/ke-hoach-thiet-ke.md` trong cùng PR |

- Quy tắc theo vùng code tự nạp từ `.claude/rules/` khi mở file: `backend.md`, `frontend.md`, `deploy.md`. Đọc chúng thay vì đoán quy ước.
- Hook `.claude/hooks/guard-migrations.mjs` chặn sửa file Flyway đã commit; muốn đổi schema thì tạo migration mới.
- Không commit thẳng lên `main`: tạo nhánh `feat/<service>-<topic>` (hoặc `fix/`, `chore/`), commit message tiếng Việt. Push, tạo PR và lệnh tác động lên cluster luôn hỏi trước (xem `permissions.ask` trong `.claude/settings.json`).
- Kết quả test phải là kết quả thật: nói rõ lệnh nào đã chạy, lệnh nào chưa.

## Chế độ tự chủ

Chủ shop đã chốt thiết kế và yêu cầu làm liên tục không hỏi. Khi được giao "làm tiếp" / "chạy backlog":

1. Đọc `docs/backlog.md`, lấy task đầu tiên chưa tick. Đọc `docs/decisions.md` để lấy mặc định.
2. Làm theo `/ecom-feature` (BE) hoặc quy tắc `frontend.md` (FE) trên nhánh riêng; test xanh; `/ecom-review` tự sửa mục *phải sửa*.
3. Fast-forward vào `main`, push (quyết định P-01). Tick task trong backlog ngay trong cùng commit.
4. Lệch thiết kế hoặc quyết định mới → ghi `docs/ke-hoach-thiet-ke.md` / `docs/decisions.md` trong cùng commit.
5. Sang task tiếp theo. **Không hỏi** trừ các điều kiện dừng ở P-02 (credential, xóa dữ liệu, tốn tiền, CI đỏ 2 lần, checkpoint).
6. Khi dừng ở checkpoint: báo cáo ngắn gồm việc đã xong, cách demo, quyết định mới, việc chưa làm.

## Lệnh thường dùng

Máy dev không cài Maven toàn cục: luôn dùng wrapper trong `backend/` (`./mvnw` hoặc `mvnw.cmd` trên Windows).

```bash
# Backend (chạy trong backend/)
./mvnw verify                                   # build + test toàn bộ, gồm kiểm tra ranh giới module
./mvnw -pl catalog-service -am verify           # một service và các module common nó phụ thuộc
./mvnw -pl common/common-security -Dtest=KeycloakRolesConverterTest test          # một test class
./mvnw -pl order-service -am -Dtest=ModularityTests -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw install -DskipTests && ./mvnw -pl catalog-service spring-boot:run          # chạy một service (cần hạ tầng local)

# Frontend (chạy trong frontend/storefront/)
npm run dev | npm run build | npm run lint

# Hạ tầng local: PostgreSQL, Valkey, RabbitMQ, Keycloak (import realm), Mailpit
docker compose -f local/docker-compose.yml up -d
```

Port local: gateway 8080, customer 8091, catalog 8092, order 8093, notification 8094, Keycloak 8180, Valkey 6382, Mailpit UI 8026, storefront 3000 (808x, 6379–6381, 8025 đã bị các dự án khác trên máy chiếm). Trên K8s mọi service chạy 8080 (`SERVER_PORT` từ ConfigMap).

## Phiên bản

Java 21, Spring Boot **4.0.x**, Spring Cloud **2025.1.x**, Spring Modulith **2.0.x**, Next.js 16. Chưa nâng lên Boot 4.1: Spring Cloud tương ứng (2026.0) mới có bản milestone.
Boot 4 đổi tên starter: dùng `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, `spring-boot-starter-security-oauth2-resource-server` / `-oauth2-client`, `spring-boot-starter-opentelemetry`. Package auto-config của Boot cũng đã đổi (ví dụ `org.springframework.boot.security.autoconfigure...`); tra tên thật trong jar ở `~/.m2` thay vì đoán. Đã xác minh:
- `@EntityScan` → `org.springframework.boot.persistence.autoconfigure.EntityScan`
- `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure` (cần dep test `spring-boot-starter-data-jpa-test`); `@AutoConfigureTestDatabase` → `org.springframework.boot.jdbc.test.autoconfigure`
- `@WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure` (cần dep test `spring-boot-starter-webmvc-test`); `@MockitoBean` là của Spring Framework: `org.springframework.test.context.bean.override.mockito`
- Testcontainers 2.x do Boot quản lý version, artifact có tiền tố: `testcontainers-postgresql`, `testcontainers-junit-jupiter`, `testcontainers-rabbitmq`
- Slice test không nạp auto-config của dự án: thêm `@ImportAutoConfiguration(JpaAuditingAutoConfiguration.class)` cho `@DataJpaTest`, `@ImportAutoConfiguration({CommonWebAutoConfiguration.class, ResourceServerAutoConfiguration.class})` + property `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` cho `@WebMvcTest`
- Surefire và `spring-boot:run` chạy với `-Duser.timezone=UTC` (parent pom); không bỏ vì PostgreSQL từ chối múi giờ "Asia/Saigon" mà JVM trên Windows báo. Chạy từ IDE thì thêm VM option này

## Kiến trúc

### Service và module
- `gateway` là **reactive** (Spring Cloud Gateway Server WebFlux, route khai báo trong `application.yml` dưới `spring.cloud.gateway.server.webflux`). 4 service còn lại là **servlet**.
- Mỗi service là một modular monolith: **mỗi package con trực tiếp của package gốc** (ví dụ `vn.ecommerce.catalog.inventory`) là một module Spring Modulith. Module chỉ gọi nhau qua type ở package gốc của module hoặc qua event. `ModularityTests` (`ApplicationModules.verify()`) làm build fail khi vi phạm. Đừng tạo package con mới ở gốc cho code hạ tầng, vì nó sẽ thành module; đặt config ở package gốc của service.
- Module đã chia sẵn theo thiết kế: customer `{profile,address,wishlist,staffadmin}`, catalog `{product,inventory,search,review,media,messaging}`, order `{cart,promotion,checkout,orders,payment,shipping}`, notification `{dispatch,channel,template}`.
- Mẫu gọi chéo module (catalog): `product` để lộ `VariantLookupApi`/`VariantInfo` ở package gốc; `inventory` dùng nó thay vì join bảng của product. Module `messaging` cung cấp `ProcessedEvents.markProcessed(eventId, consumer)` (propagation MANDATORY, gọi trong transaction của consumer) và exchange + converter JSON; mỗi module consumer tự khai báo queue + DLQ (`OrderEventsTopology`).
- Thao tác tồn kho (`ReservationServiceImpl`) không dùng `@Transactional` ở class: mỗi lần thử là một `TransactionTemplate.execute` riêng để retry khi optimistic lock thất bại; test tranh chấp thật ở `InventoryReservationDbTest` (`@SpringBootTest` + Testcontainers, tắt listener RabbitMQ và externalization bằng property).

### Thư viện common (tự đăng ký qua `AutoConfiguration.imports`)
- `common-web`: chỉ cho servlet. `GlobalExceptionHandler` là nơi duy nhất log stack trace. Service báo lỗi bằng cách ném `BadRequestException` / `NotFoundException` / `ConflictException` / `ForbiddenException(messageKey, args...)`. `messageKey` vừa là `code` trả cho FE, vừa là key i18n. Mỗi service cấu hình `spring.messages.basename: messages,common-messages`. Response lỗi và header `X-Trace-Id` mang `traceId` lấy từ MDC.
- `common-security`: `ResourceServerAutoConfiguration` tạo security chain mặc định cho service servlet. Nó phải chạy **trước** các auto-config của Boot tạo chain mặc định (khai báo `beforeName`). `ResourceServerAutoConfigurationTest` canh điều này; thêm starter security mới thì kiểm tra lại test đó. Quy tắc của chain:
  - `/actuator/**` mở.
  - `/internal/**` cần role `INTERNAL`, gán cho service account client credentials (không dùng scope).
  - `/api/admin/**` cần `STAFF` hoặc `ADMIN`.
  - Path công khai khai báo bằng `ecommerce.security.public-paths` / `public-get-paths`.
  - Role đọc từ claim phẳng `roles` (mapper `realm-roles-flat` của Keycloak) qua `KeycloakRolesConverter`.
  - Gateway không dùng chain này; nó có `GatewaySecurityConfig` riêng nhưng tái dùng converter.
- `common-jpa`: `AuditableEntity` (id UUID sinh ở app, audit cột, `@Version`), `SoftDeletableEntity` (`markDeleted`), `CurrentAuditorProvider` lấy `sub` từ JWT (job/consumer = `system`), `AuditRevisionEntity` cho Envers (bảng `revinfo` có `username`), `InBatches` chia lô `IN`. Service dùng phải khai báo `@EntityScan({"vn.ecommerce.<svc>", "vn.ecommerce.common.jpa"})` và tạo bảng `revinfo` + `<bảng>_aud` bằng Flyway (Envers cấu hình suffix `_aud`, cột `rev`/`revtype` trong `application.yml`).
- `common-events`: record event thuần Java + `EventRoutes` (topic exchange `ecommerce.events`, routing key `<domain>.<event>.v<n>`). Chỉ thêm field; đổi nghĩa thì tạo event `.v2`. Consumer phải đặt `DefaultJacksonJavaTypeMapper.setTrustedPackages(EventRoutes.EVENT_PACKAGES...)` (so khớp đúng tên package, không tính package con; thiếu là message vào DLQ); thêm package event mới thì thêm vào `EVENT_PACKAGES`. `RabbitConfigTest` của notification canh việc này.

### Auth (Keycloak)
- Realm `ecommerce`. Service kiểm tra `iss` theo URL public (`KEYCLOAK_ISSUER_URI`), nhưng tải JWKS qua địa chỉ nội bộ (`KEYCLOAK_JWK_SET_URI`); audience bắt buộc là `ecommerce-api`.
- `CurrentUser.requireId()` trả `sub` của Keycloak, dùng làm `customerId` ở mọi service. Không nhận id người dùng từ request body.
- order-service (gọi catalog `/internal`) và customer-service (Keycloak Admin API) có registration OAuth2 client_credentials trong `application.yml`.
- customer-service: `customer_profile.id` = `sub` Keycloak (`AuditableEntity.assignId`), hồ sơ tạo JIT ở `ProfileApi.ensureCurrentProfile()`; module khác (address, wishlist, staffadmin) gọi `ProfileApi`, không đụng repository của profile. Keycloak Admin API chỉ đi qua `KeycloakAdminGateway` (impl `KeycloakAdminRestClient`, bean `RestClient` tên `keycloakAdminHttpClient` có `OAuth2ClientHttpRequestInterceptor`); Boot 4 cần thêm `spring-boot-starter-restclient` mới có bean `RestClient.Builder`. Dữ liệu tham chiếu tỉnh/phường nằm ở `db/reference/R__admin_divisions.sql` (Flyway `locations` gồm cả `classpath:db/reference`), sinh lại bằng script scratchpad `divisions-to-sql.mjs`, mã zero-pad theo GSO (B-13).
- `PageResponse` (common-web, không phụ thuộc Spring Data) là kiểu phân trang chung cho service mới; catalog vẫn dùng `PageView` riêng của module product.
- order-service: checkout là orchestrator (`CheckoutServiceImpl`) gọi catalog/customer **ngoài** transaction rồi ghi đơn + voucher + payment + dọn giỏ trong một `TransactionTemplate`; lỗi sau khi giữ hàng thì `CatalogClient.release`. Đổi trạng thái đơn chỉ qua `OrderStateMachine` (kiểm tra `OrderStatus.canTransitionTo`, ghi `order_status_history`, phát event). Module phụ thuộc một chiều: checkout → {cart, promotion, shipping, payment, orders, catalogclient, customerclient}; payment → orders; shipping báo về orders bằng Spring event `ShipmentStatusChanged` (không gọi ngược). `admin_audit_log` dùng `AdminAuditRecorder` của common-jpa (mỗi service tự tạo bảng trong migration). Webhook ký HMAC bằng `HmacSignatures` (common-web).
- Realm dev ở `local/keycloak/ecommerce-realm.json`; client secret trong đó chỉ dùng cho dev.

### Dữ liệu và event
- Mỗi service một database/user PostgreSQL (local: `local/postgres/init`). `ddl-auto: validate`, schema chỉ đổi qua Flyway `src/main/resources/db/migration`. Migration đã merge là bất biến.
- Outbox = Spring Modulith Event Publication Registry (**JDBC**, không phải JPA). Bảng `event_publication` (schema v2) được tạo bằng Flyway `V1__init_event_tables.sql`, không bật schema-initialization của Modulith. Service consume event có thêm bảng `processed_event (event_id, consumer)` để xử lý idempotent.
- Event được externalize sang RabbitMQ (`spring.modulith.events.externalization.enabled`); observation của RabbitMQ đã bật để trace đi xuyên qua event.

### Logging và vận hành
- Log JSON (ECS) chỉ bật khi chạy trên Kubernetes (`on-cloud-platform: kubernetes` trong `application.yml`); chạy local in text. Quy tắc nội dung log (không log token/PII, label Loki...) nằm ở mục 12 của tài liệu thiết kế.
- Tracing export OTLP tắt mặc định (`TRACING_EXPORT_ENABLED`).

### Deploy
- `deploy/charts/spring-service` là chart dùng chung cho cả 5 service. `deploy/environments/{staging,prod}/<service>.yaml` là values riêng; CI cập nhật `image.tag` (git SHA).
- Tên Service trong namespace môi trường: `pg-rw`, `rabbitmq`, `valkey`, `keycloak-service`, secret `<service>-secrets` (xem `deploy/README.md`).
- Image build bằng Jib (`./mvnw package jib:build -Dimage.registry=... -Dimage.tag=...`), không cần Dockerfile.
- `deploy/{keycloak,data,platform,argocd}` và `infra/k3s` mới là thư mục trống.
- Placeholder cần thay: registry `ghcr.io/your-org`, domain `example.vn`.

## Frontend

`frontend/storefront/AGENTS.md`: Next.js 16.4 có breaking change so với dữ liệu huấn luyện. **Đọc hướng dẫn trong `node_modules/next/dist/docs/` trước khi viết code** và chú ý các cảnh báo deprecation. Config đang bật `cacheComponents`, `partialPrefetching` và `output: "standalone"`. Auth.js v5 theo mô hình BFF (`src/auth.ts`): session trả cho trình duyệt chỉ có user + `roles`, không có token. Ở server lấy token bằng `getAccessToken()` / gọi API cần đăng nhập bằng `authFetch()` (`src/lib/session.ts`), trang cần đăng nhập gọi `requireSession`/`requireStaff` trong component nằm dưới `<Suspense>` (Cache Components). `src/proxy.ts` (Next 16 đổi middleware thành proxy) làm mới token và chép cookie mới vào request; đừng làm mới token ở chỗ khác vì refresh token xoay vòng. Trang cửa hàng nằm trong route group `src/app/(shop)` (layout có header/footer), `/admin` có layout riêng; trang mới của cửa hàng đặt trong `(shop)`. Trong Git Bash, đặt `MSYS_NO_PATHCONV=1` khi truyền đường dẫn dạng `/admin` cho lệnh/script.
