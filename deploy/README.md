# deploy/

Manifest cho k3s, quản lý theo GitOps (Argo CD đồng bộ từ thư mục này). Xem thiết kế: `docs/ke-hoach-thiet-ke.md` mục 9–10, quyết định B-26.

| Thư mục | Nội dung |
|---|---|
| `argocd/` | `root.yaml` (app-of-apps) và `apps/`: `platform.yaml` (operator), `staging.yaml`, `prod.yaml` (secret → dữ liệu → Keycloak → service + ingress) |
| `platform/` | Namespace `platform`/`staging`/`prod`, ClusterIssuer Let's Encrypt, NetworkPolicy default-deny, phần cấp cluster của Keycloak Operator |
| `secrets/` | `seal-env.sh <env>` sinh SealedSecret vào `secrets/<env>/` (chỉ file đã niêm phong được commit) |
| `data/` | CloudNativePG cluster `pg` + 5 database + backup R2 (plugin Barman Cloud), RabbitMQ, Valkey; overlay `prod`/`staging` |
| `keycloak/` | Keycloak Operator theo namespace, CR `Keycloak`, realm as code (`base/realm`, keycloak-config-cli), Ingress chỉ mở `/realms`, `/resources` |
| `ingress/` | Ingress public mỗi môi trường: `api.<domain>` → gateway, `shop.<domain>` → storefront |
| `charts/spring-service/` | Helm chart dùng chung cho mọi service Spring Boot (ApplicationSet `services-<env>` dùng chart này + values) |
| `environments/{staging,prod}/` | Values từng service theo môi trường; CI cập nhật `image.tag` |

Chưa có: chart/image storefront (Service `storefront` trong Ingress chưa tồn tại), theme Keycloakify (E6.4), Grafana Alloy.

## Quy ước tên trong mỗi namespace môi trường (`staging`, `prod`)

| Thành phần | Tên Service | Ghi chú |
|---|---|---|
| PostgreSQL (CNPG) | `pg-rw` | Database `customer_db`, `catalog_db`, `order_db`, `notification_db`, `keycloak_db`; user `customer`, `catalog`, `orders`, `notification`, `keycloak` |
| RabbitMQ | `rabbitmq` | |
| Valkey | `valkey` | Rate limit của gateway |
| Keycloak | `keycloak-service` | Tên mặc định do Keycloak Operator tạo từ CR `keycloak` |
| Secret của service | `<service>-secrets` | SealedSecret sinh bằng `secrets/seal-env.sh` |

Host: prod `shop.`, `api.`, `auth.example.vn`; staging `shop-staging.`, `api-staging.`, `auth-staging.example.vn` (subdomain một cấp để khớp chứng chỉ Cloudflare). Thay `example.vn` khi có domain.

## Dựng cluster lần đầu

Chỉ chạy khi chủ shop yêu cầu (lệnh tác động lên cluster thật).

```bash
# 1. Argo CD v3.5.4 vào namespace argocd
kubectl create namespace argocd
kubectl apply -n argocd --server-side -f https://raw.githubusercontent.com/argoproj/argo-cd/v3.5.4/manifests/install.yaml

# 2. Cho Argo CD tính health của Application con, để sync-wave của app-of-apps chờ nhau
kubectl -n argocd patch configmap argocd-cm --type merge -p '{"data":{"resource.customizations.health.argoproj.io_Application":"hs = {}\nhs.status = \"Progressing\"\nhs.message = \"\"\nif obj.status ~= nil and obj.status.health ~= nil then\n  hs.status = obj.status.health.status\n  if obj.status.health.message ~= nil then hs.message = obj.status.health.message end\nend\nreturn hs\n"}}'

# 3. App-of-apps: operator + nền tảng chạy trước (wave -3, -2)
kubectl apply -f deploy/argocd/root.yaml

# 4. Khi sealed-secrets chạy: sinh secret cho từng môi trường, commit, Argo CD tự đồng bộ
kubeseal --fetch-cert --controller-namespace platform --controller-name sealed-secrets-controller > pub.pem
SEALED_CERT=pub.pem R2_ACCESS_KEY_ID=... R2_SECRET_ACCESS_KEY=... SMTP_HOST=... STOREFRONT_URL=https://shop.example.vn \
  deploy/secrets/seal-env.sh prod
```

Sau khi Keycloak chạy: đăng nhập admin console qua `kubectl -n prod port-forward svc/keycloak-service 8080`, tạo admin thật có OTP, khóa tài khoản `bootstrap-admin` (thiết kế mục 11).

## Kiểm tra trước khi merge

```bash
kubectl kustomize deploy/data/overlays/prod          # tương tự platform, keycloak/overlays/<env>, ingress/<env>
helm template order-service deploy/charts/spring-service -f deploy/environments/prod/order-service.yaml --namespace prod
```

Realm as code sinh từ realm dev: sửa `local/keycloak/ecommerce-realm.json` xong chạy `node deploy/keycloak/base/realm/build-realm.mjs`.

Yêu cầu Kubernetes ≥ 1.30 (preStop dùng action `sleep`).
