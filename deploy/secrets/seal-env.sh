#!/usr/bin/env bash
# Tạo toàn bộ SealedSecret cho một môi trường vào deploy/secrets/<env>/ (file đã niêm phong, commit được).
#
#   deploy/secrets/seal-env.sh staging            # dùng chứng chỉ lấy trực tiếp từ cluster
#   SEALED_CERT=pub.pem deploy/secrets/seal-env.sh prod   # hoặc chứng chỉ đã tải về (kubeseal --fetch-cert)
#
# Mật khẩu nội bộ (DB, RabbitMQ, client secret Keycloak, admin Keycloak, secret webhook giả) sinh ngẫu nhiên mỗi lần
# chạy, nên chỉ chạy lại khi muốn xoay vòng tất cả. Credential bên ngoài đọc từ biến môi trường của người chạy:
#   R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, SMTP_HOST, SMTP_PORT, SMTP_FROM, SMTP_USERNAME, SMTP_PASSWORD,
#   VNPAY_HASH_SECRET, MOMO_ACCESS_KEY, MOMO_SECRET_KEY, STOREFRONT_URL   (để trống = giá trị rỗng / tắt tích hợp)
# Không bao giờ commit Secret chưa niêm phong; script chỉ ghi SealedSecret.
set -euo pipefail

ENV="${1:?Cách dùng: seal-env.sh <staging|prod>}"
case "$ENV" in staging|prod) ;; *) echo "Môi trường không hợp lệ: $ENV" >&2; exit 1 ;; esac
command -v kubeseal >/dev/null || { echo "Cần cài kubeseal" >&2; exit 1; }
command -v kubectl >/dev/null || { echo "Cần cài kubectl" >&2; exit 1; }

OUT="$(cd "$(dirname "$0")" && pwd)/$ENV"
mkdir -p "$OUT"
SEAL_ARGS=(--format yaml --controller-namespace platform --controller-name sealed-secrets-controller)
[ -n "${SEALED_CERT:-}" ] && SEAL_ARGS+=(--cert "$SEALED_CERT")

rand() { openssl rand -base64 36 | tr -dc 'A-Za-z0-9' | head -c 32; }

# seal <tên> <type> KEY=VALUE...  → $OUT/<tên>.yaml
seal() {
  local name="$1" type="$2"; shift 2
  local args=()
  for kv in "$@"; do args+=(--from-literal="$kv"); done
  kubectl create secret generic "$name" --namespace "$ENV" --type "$type" "${args[@]}" --dry-run=client -o yaml \
    | kubeseal "${SEAL_ARGS[@]}" > "$OUT/$name.yaml"
  echo "  $OUT/$name.yaml"
}

echo "Niêm phong secret cho $ENV:"

# PostgreSQL: user theo postgres-cluster.yaml (managed.roles), service dùng cùng user/mật khẩu
declare -A DB_USER=([customer]=customer [catalog]=catalog [order]=orders [notification]=notification [keycloak]=keycloak)
declare -A DB_PASS
for db in customer catalog order notification keycloak; do
  DB_PASS[$db]="$(rand)"
  seal "pg-$db-credentials" kubernetes.io/basic-auth "username=${DB_USER[$db]}" "password=${DB_PASS[$db]}"
done

RABBIT_PASS="$(rand)"
seal rabbitmq-credentials Opaque username=ecommerce "password=$RABBIT_PASS"
seal r2-backup-credentials Opaque "ACCESS_KEY_ID=${R2_ACCESS_KEY_ID:-}" "ACCESS_SECRET_KEY=${R2_SECRET_ACCESS_KEY:-}"

# Keycloak: admin bootstrap (đổi/khóa sau lần đăng nhập đầu) + giá trị điền vào realm as code
STOREFRONT_SECRET="$(rand)"; ORDER_CLIENT_SECRET="$(rand)"; CUSTOMER_CLIENT_SECRET="$(rand)"
seal keycloak-bootstrap-admin Opaque username=bootstrap-admin "password=$(rand)"
seal keycloak-realm-secrets Opaque \
  "STOREFRONT_URL=${STOREFRONT_URL:-}" "STOREFRONT_CLIENT_SECRET=$STOREFRONT_SECRET" \
  "ORDER_SERVICE_CLIENT_SECRET=$ORDER_CLIENT_SECRET" "CUSTOMER_SERVICE_CLIENT_SECRET=$CUSTOMER_CLIENT_SECRET" \
  "SMTP_HOST=${SMTP_HOST:-}" "SMTP_PORT=${SMTP_PORT:-587}" "SMTP_FROM=${SMTP_FROM:-}" "SMTP_FROM_NAME=${SMTP_FROM_NAME:-}" \
  "SMTP_USERNAME=${SMTP_USERNAME:-}" "SMTP_PASSWORD=${SMTP_PASSWORD:-}"

# Secret của từng service (envFromSecrets trong deploy/environments/<env>/<service>.yaml)
seal customer-service-secrets Opaque "DB_USERNAME=customer" "DB_PASSWORD=${DB_PASS[customer]}" \
  RABBITMQ_USERNAME=ecommerce "RABBITMQ_PASSWORD=$RABBIT_PASS" "KEYCLOAK_CLIENT_SECRET=$CUSTOMER_CLIENT_SECRET"
seal catalog-service-secrets Opaque "DB_USERNAME=catalog" "DB_PASSWORD=${DB_PASS[catalog]}" \
  RABBITMQ_USERNAME=ecommerce "RABBITMQ_PASSWORD=$RABBIT_PASS"
PAYMENT_MOCK_SECRET="$(rand)"
seal order-service-secrets Opaque "DB_USERNAME=orders" "DB_PASSWORD=${DB_PASS[order]}" \
  RABBITMQ_USERNAME=ecommerce "RABBITMQ_PASSWORD=$RABBIT_PASS" "KEYCLOAK_CLIENT_SECRET=$ORDER_CLIENT_SECRET" \
  "PAYMENT_MOCK_SECRET=$PAYMENT_MOCK_SECRET" "SHIPPING_MOCK_SECRET=$(rand)" \
  "VNPAY_HASH_SECRET=${VNPAY_HASH_SECRET:-}" "MOMO_ACCESS_KEY=${MOMO_ACCESS_KEY:-}" "MOMO_SECRET_KEY=${MOMO_SECRET_KEY:-}"
seal notification-service-secrets Opaque "DB_USERNAME=notification" "DB_PASSWORD=${DB_PASS[notification]}" \
  RABBITMQ_USERNAME=ecommerce "RABBITMQ_PASSWORD=$RABBIT_PASS" \
  "SMTP_USERNAME=${SMTP_USERNAME:-}" "SMTP_PASSWORD=${SMTP_PASSWORD:-}"

# Storefront (BFF): khóa cookie session, secret client storefront-web (trùng realm), secret revalidate.
# Cổng thanh toán giả chỉ ở staging (prod đặt PAYMENT_MOCK_ENABLED=false nên storefront không cần secret này).
STOREFRONT_EXTRA=()
[ "$ENV" = staging ] && STOREFRONT_EXTRA+=("PAYMENT_MOCK_SECRET=$PAYMENT_MOCK_SECRET")
seal storefront-secrets Opaque "AUTH_SECRET=$(openssl rand -base64 32)" "AUTH_KEYCLOAK_SECRET=$STOREFRONT_SECRET" \
  "REVALIDATE_SECRET=$(rand)" "${STOREFRONT_EXTRA[@]}"

# Danh sách file cho kustomization của môi trường
( cd "$OUT" && { echo "apiVersion: kustomize.config.k8s.io/v1beta1"; echo "kind: Kustomization"; echo "resources:"; \
  for f in *.yaml; do [ "$f" = kustomization.yaml ] || echo "  - $f"; done; } > kustomization.yaml )
echo "Xong. Kiểm tra rồi commit deploy/secrets/$ENV/ (chỉ chứa SealedSecret)."
