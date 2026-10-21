# ecommerce

Website bán thời trang, phụ kiện, trang sức. Microservice Spring Boot (5 service) + Keycloak + Next.js, chạy trên k3s.
Thiết kế đầy đủ: [docs/ke-hoach-thiet-ke.md](docs/ke-hoach-thiet-ke.md).

## Cấu trúc

```
backend/                 Maven multi-module (Java 21, Spring Boot 4.0, Spring Cloud 2025.1, Spring Modulith 2.0)
  common/common-web        ErrorResponse có traceId, exception theo HTTP, GlobalExceptionHandler, header X-Trace-Id
  common/common-security   Resource server JWT Keycloak: claim roles -> ROLE_*, CurrentUser, security chain mặc định
  common/common-events     Hợp đồng event giữa service (record) + exchange/routing key RabbitMQ
  gateway                  Spring Cloud Gateway: route, JWT, rate limit (Valkey), CORS          :8080
  customer-service         profile, address, wishlist, staffadmin                               :8091
  catalog-service          product, inventory, search, review, media                            :8092
  order-service            cart, promotion, checkout, orders, payment, shipping                 :8093
  notification-service     dispatch, channel, template                                          :8094
frontend/
  storefront/              Next.js 16 (App Router, TypeScript, Tailwind), gồm cả /admin          :3000
  keycloak-theme/          Theme đăng nhập Keycloakify (chưa khởi tạo)
local/                   docker-compose cho dev: PostgreSQL, Valkey, RabbitMQ, Keycloak, Mailpit
deploy/                  Helm chart dùng chung + values staging/prod (xem deploy/README.md)
infra/k3s/               Script dựng VPS + k3s (chưa làm)
.github/workflows/       CI backend
```

## Chạy local

Yêu cầu: JDK 21, Node.js 20+, Docker.

```bash
# 1. Hạ tầng
docker compose -f local/docker-compose.yml up -d

# 2. Backend (mỗi service một terminal, hoặc chạy từ IDE)
cd backend
./mvnw install -DskipTests                  # lần đầu, để cài các module common vào ~/.m2
./mvnw -pl catalog-service spring-boot:run  # tương tự cho gateway, customer-service, order-service, notification-service

# 3. Frontend
cd frontend/storefront
npm install
npm run dev
```

| Công cụ | URL | Tài khoản |
|---|---|---|
| Keycloak admin | http://localhost:8180/admin | `admin` / `admin` |
| Keycloak realm `ecommerce` | http://localhost:8180/realms/ecommerce/account | `customer@example.com` / `Customer123`, `admin@example.com` / `Admin12345` |
| RabbitMQ | http://localhost:15672 | `ecommerce` / `ecommerce` |
| Mailpit (email Keycloak gửi) | http://localhost:8026 | |
| Swagger UI từng service | http://localhost:809x/swagger-ui.html | |

Mật khẩu và client secret trong `local/` chỉ dùng cho dev, không dùng ở staging/prod.

Storefront đọc biến môi trường từ `frontend/storefront/.env.local` (không commit):

| Biến | Dev | Ghi chú |
|---|---|---|
| `AUTH_SECRET` | chuỗi ngẫu nhiên (`npx auth secret`) | Khóa mã hóa cookie session, bắt buộc |
| `AUTH_KEYCLOAK_ID` | `storefront-web` | |
| `AUTH_KEYCLOAK_SECRET` | `storefront-web-dev-secret` | Secret dev trong realm `local/keycloak` |
| `AUTH_KEYCLOAK_ISSUER` | `http://localhost:8180/realms/ecommerce` | |
| `AUTH_TRUST_HOST` | `true` khi chạy `next start`/container | Sau Traefik ở K8s cũng cần |
| `API_BASE_URL` | `http://localhost:8080` | Gateway, gọi từ server |
| `NEXT_PUBLIC_SITE_URL` | `http://localhost:3000` | URL quay về sau khi đăng xuất Keycloak |
| `PAYMENT_MOCK_SECRET` | `payment-mock-dev-secret` | Bật cổng thanh toán giả; phải trùng `ecommerce.payment.mock.secret` của order-service. Không đặt ở production |

## Build và test

```bash
cd backend
./mvnw verify          # build + unit test + kiểm tra ranh giới module (Spring Modulith)
```

Tài liệu module sinh ra ở `backend/<service>/target/spring-modulith-docs`.

## Quy ước

- Mỗi package con trực tiếp của service là một module Spring Modulith. Module chỉ gọi nhau qua type ở package gốc của module hoặc qua event; `ModularityTests` chặn vi phạm.
- Lỗi trả về luôn theo `ErrorResponse` (`code`, `message`, `traceId`, `errors`); service chỉ ném `BadRequestException` / `NotFoundException` / `ConflictException` / `ForbiddenException` với message key.
- Event giữa service: record trong `common-events`, routing key `<domain>.<event>.v<n>`, ghi qua outbox của Spring Modulith (bảng `event_publication`).
- Migration: Flyway `src/main/resources/db/migration`, chỉ thay đổi tương thích ngược.
- `/internal/**` chỉ cho service account có role `INTERNAL`; `/api/admin/**` cho `STAFF`/`ADMIN`.
