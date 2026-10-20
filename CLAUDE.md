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

Port local: gateway 8080, customer 8081, catalog 8082, order 8083, notification 8084, Keycloak 8180, storefront 3000. Trên K8s mọi service chạy 8080 (`SERVER_PORT` từ ConfigMap).

## Phiên bản

Java 21, Spring Boot **4.0.x**, Spring Cloud **2025.1.x**, Spring Modulith **2.0.x**, Next.js 16. Chưa nâng lên Boot 4.1: Spring Cloud tương ứng (2026.0) mới có bản milestone.
Boot 4 đổi tên starter: dùng `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, `spring-boot-starter-security-oauth2-resource-server` / `-oauth2-client`, `spring-boot-starter-opentelemetry`. Package auto-config của Boot cũng đã đổi (ví dụ `org.springframework.boot.security.autoconfigure...`); tra tên thật trong jar ở `~/.m2` thay vì đoán.

## Kiến trúc

### Service và module
- `gateway` là **reactive** (Spring Cloud Gateway Server WebFlux, route khai báo trong `application.yml` dưới `spring.cloud.gateway.server.webflux`). 4 service còn lại là **servlet**.
- Mỗi service là một modular monolith: **mỗi package con trực tiếp của package gốc** (ví dụ `vn.ecommerce.catalog.inventory`) là một module Spring Modulith. Module chỉ gọi nhau qua type ở package gốc của module hoặc qua event. `ModularityTests` (`ApplicationModules.verify()`) làm build fail khi vi phạm. Đừng tạo package con mới ở gốc cho code hạ tầng, vì nó sẽ thành module; đặt config ở package gốc của service.
- Module đã chia sẵn theo thiết kế: customer `{profile,address,wishlist,staffadmin}`, catalog `{product,inventory,search,review,media}`, order `{cart,promotion,checkout,orders,payment,shipping}`, notification `{dispatch,channel,template}`.

### Thư viện common (tự đăng ký qua `AutoConfiguration.imports`)
- `common-web`: chỉ cho servlet. `GlobalExceptionHandler` là nơi duy nhất log stack trace. Service báo lỗi bằng cách ném `BadRequestException` / `NotFoundException` / `ConflictException` / `ForbiddenException(messageKey, args...)`. `messageKey` vừa là `code` trả cho FE, vừa là key i18n. Mỗi service cấu hình `spring.messages.basename: messages,common-messages`. Response lỗi và header `X-Trace-Id` mang `traceId` lấy từ MDC.
- `common-security`: `ResourceServerAutoConfiguration` tạo security chain mặc định cho service servlet. Nó phải chạy **trước** các auto-config của Boot tạo chain mặc định (khai báo `beforeName`). `ResourceServerAutoConfigurationTest` canh điều này; thêm starter security mới thì kiểm tra lại test đó. Quy tắc của chain:
  - `/actuator/**` mở.
  - `/internal/**` cần role `INTERNAL`, gán cho service account client credentials (không dùng scope).
  - `/api/admin/**` cần `STAFF` hoặc `ADMIN`.
  - Path công khai khai báo bằng `ecommerce.security.public-paths` / `public-get-paths`.
  - Role đọc từ claim phẳng `roles` (mapper `realm-roles-flat` của Keycloak) qua `KeycloakRolesConverter`.
  - Gateway không dùng chain này; nó có `GatewaySecurityConfig` riêng nhưng tái dùng converter.
- `common-events`: record event thuần Java + `EventRoutes` (topic exchange `ecommerce.events`, routing key `<domain>.<event>.v<n>`). Chỉ thêm field; đổi nghĩa thì tạo event `.v2`.

### Auth (Keycloak)
- Realm `ecommerce`. Service kiểm tra `iss` theo URL public (`KEYCLOAK_ISSUER_URI`), nhưng tải JWKS qua địa chỉ nội bộ (`KEYCLOAK_JWK_SET_URI`); audience bắt buộc là `ecommerce-api`.
- `CurrentUser.requireId()` trả `sub` của Keycloak, dùng làm `customerId` ở mọi service. Không nhận id người dùng từ request body.
- order-service (gọi catalog `/internal`) và customer-service (Keycloak Admin API) có registration OAuth2 client_credentials trong `application.yml`.
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

`frontend/storefront/AGENTS.md`: Next.js 16.4 có breaking change so với dữ liệu huấn luyện. **Đọc hướng dẫn trong `node_modules/next/dist/docs/` trước khi viết code** và chú ý các cảnh báo deprecation. Config đang bật `cacheComponents`, `partialPrefetching` và `output: "standalone"`. Auth sẽ dùng Auth.js theo mô hình BFF (token không lộ ra JS phía trình duyệt; xem mục 6 của thiết kế). Trang `/admin` nằm trong cùng app.
