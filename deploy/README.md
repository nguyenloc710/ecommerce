# deploy/

Manifest cho k3s, quản lý theo GitOps (Argo CD đồng bộ từ thư mục này). Xem thiết kế: `docs/ke-hoach-thiet-ke.md` mục 9–10.

| Thư mục | Nội dung | Trạng thái |
|---|---|---|
| `charts/spring-service/` | Helm chart dùng chung cho mọi service Spring Boot: ConfigMap, Deployment (probes, graceful shutdown, non-root, read-only FS), Service, HPA, PDB, NetworkPolicy, ServiceAccount | Đã có |
| `environments/{staging,prod}/` | Values từng service theo môi trường; CI cập nhật `image.tag` | Đã có (domain `example.vn` là placeholder) |
| `keycloak/` | CR `Keycloak` (Keycloak Operator), realm as code (keycloak-config-cli), Job áp dụng realm | Chưa làm |
| `data/` | CloudNativePG cluster `pg` (backup lên R2), Valkey, RabbitMQ | Chưa làm |
| `platform/` | cert-manager, Sealed Secrets, Grafana Alloy, Keycloak Operator, NetworkPolicy default-deny | Chưa làm |
| `argocd/` | App-of-apps trỏ vào các thư mục trên | Chưa làm |

## Quy ước tên trong mỗi namespace môi trường (`staging`, `prod`)

| Thành phần | Tên Service | Ghi chú |
|---|---|---|
| PostgreSQL (CNPG) | `pg-rw` | Mỗi service một database: `customer_db`, `catalog_db`, `order_db`, `notification_db`, `keycloak_db` |
| RabbitMQ | `rabbitmq` | |
| Valkey | `valkey` | Rate limit của gateway |
| Keycloak | `keycloak-service` | Tên mặc định do Keycloak Operator tạo |
| Secret của service | `<service>-secrets` | SealedSecret: `DB_USERNAME`, `DB_PASSWORD`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `KEYCLOAK_CLIENT_SECRET` |

## Render thử chart

```bash
helm template order-service deploy/charts/spring-service \
  -f deploy/environments/prod/order-service.yaml --namespace prod
```

Yêu cầu Kubernetes ≥ 1.30 (preStop dùng action `sleep`).
