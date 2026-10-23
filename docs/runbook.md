# Runbook vận hành

Xử lý sự cố và diễn tập cho cụm k3s (thiết kế mục 9–12, checklist go-live mục 17). Mọi lệnh chạy trong namespace môi trường (`staging` hoặc `prod`); đặt trước `NS=prod` hoặc `NS=staging`.

Kubectl plugin `cnpg` (`kubectl cnpg ...`) tiện nhưng không bắt buộc; mỗi bước đều có lệnh kubectl thường.

---

## 1. Diễn tập khôi phục backup (làm trước go-live và mỗi tháng một lần)

Mục tiêu: chứng minh backup trên R2 dùng được. Khôi phục vào cluster **mới** `pg-restore`, không đụng `pg` đang chạy.

1. Kiểm tra backup gần nhất đã xong:
   ```bash
   kubectl -n $NS get backups.postgresql.cnpg.io --sort-by=.metadata.creationTimestamp | tail -3   # PHASE = completed
   kubectl -n $NS get cluster pg -o jsonpath='{.status.conditions[?(@.type=="ContinuousArchiving")].status}'  # True
   ```
2. Ghi lại vài con số trên `pg` để so sánh (đơn mới nhất, số sản phẩm):
   ```bash
   kubectl -n $NS exec pg-1 -c postgres -- psql -d order_db -Atc "select count(*), max(placed_at) from orders"
   kubectl -n $NS exec pg-1 -c postgres -- psql -d catalog_db -Atc "select count(*) from product"
   ```
3. Tạo cluster khôi phục (đã gồm NetworkPolicy cho operator):
   ```bash
   kubectl apply -k deploy/data/restore/overlays/$NS
   kubectl -n $NS get cluster pg-restore -w        # chờ STATUS "Cluster in healthy state" (vài phút tùy dung lượng)
   ```
4. So sánh số liệu trên `pg-restore-1` với bước 2 (chênh vài đơn cuối là bình thường nếu WAL cuối chưa kịp đẩy):
   ```bash
   kubectl -n $NS exec pg-restore-1 -c postgres -- psql -d order_db -Atc "select count(*), max(placed_at) from orders"
   kubectl -n $NS exec pg-restore-1 -c postgres -- psql -d keycloak_db -Atc "select count(*) from user_entity"
   ```
5. Dọn dẹp:
   ```bash
   kubectl delete -k deploy/data/restore/overlays/$NS
   kubectl -n $NS delete pvc -l cnpg.io/cluster=pg-restore
   ```
6. Ghi kết quả vào `docs/go-live-checklist.md` (ngày, thời gian khôi phục, chênh lệch số liệu).

## 2. Khôi phục thật (mất dữ liệu, xóa nhầm, DB hỏng)

1. **Dừng ghi**: scale các service về 0 để không ghi thêm vào DB hỏng.
   ```bash
   kubectl -n $NS scale deploy customer-service catalog-service order-service notification-service --replicas=0
   ```
   (Argo CD sẽ scale lại khi sync. Tạm tắt auto-sync của các app service trong lúc khôi phục.)
2. Chọn thời điểm: nếu xóa nhầm lúc 10:05 thì khôi phục tới 10:04. Bỏ comment `recoveryTarget.targetTime` trong `deploy/data/restore/base/cluster-restore.yaml` (chỉ sửa ở máy chạy lệnh, không commit) rồi làm bước 3–4 của mục 1.
3. Chuyển service sang cluster đã khôi phục: đổi `DB_URL` trong `deploy/environments/$NS/*.yaml` từ `pg-rw` sang `pg-restore-rw`, đổi `spec.db.host` của Keycloak (`deploy/keycloak/overlays/$NS`), commit, bật lại sync.
   - Mật khẩu user DB là mật khẩu **tại thời điểm backup**. Nếu đã đổi mật khẩu sau đó, tạm dùng mật khẩu cũ hoặc `ALTER ROLE ... PASSWORD` trên `pg-restore`.
4. Khi ổn định: lên lịch đưa dữ liệu về lại tên `pg` (tạo `pg` mới bằng `bootstrap.recovery` từ backup của `pg-restore`) hoặc giữ `pg-restore` và cấu hình backup cho nó. Ghi quyết định vào `docs/decisions.md`.
5. Sau khôi phục: event chưa gửi trong bảng `event_publication` được gửi lại khi service khởi động (`republish-outstanding-events-on-restart`); consumer idempotent theo `processed_event` nên gửi trùng không sao. Kiểm tra DLQ RabbitMQ.

## 3. Node chết

Prod có 2 node; PostgreSQL 2 instance trên 2 node khác nhau (anti-affinity).

- **Node chứa primary chết:** CNPG tự promote replica trong khoảng 30 giây. Service `pg-rw` trỏ sang primary mới; Hikari tự kết nối lại (vài request lỗi 500 trong lúc chuyển là bình thường).
  ```bash
  kubectl -n $NS get cluster pg        # PRIMARY đã đổi sang pg-2
  kubectl get nodes                    # node NotReady
  ```
- Pod của node chết (service, Keycloak, RabbitMQ, Valkey) được k3s đưa sang node còn lại sau khoảng 5 phút (`pod-eviction-timeout`). Nếu cần nhanh: `kubectl delete pod <pod> --force --grace-period=0`.
- **PVC `local-path` gắn chặt với node:** instance PostgreSQL trên node chết không chạy được ở node khác. Khi node không thể cứu:
  ```bash
  kubectl cnpg destroy pg <tên-instance-chết> -n $NS     # hoặc: xóa PVC + pod của instance đó
  ```
  CNPG tạo lại replica mới từ primary. RabbitMQ/Valkey cùng tình trạng: RabbitMQ mất queue chưa xử lý (outbox gửi lại), Valkey mất dữ liệu thì suất flash sale tự khởi tạo lại từ DB (B-30), rate limit reset.
- Node sống lại: kiểm tra `kubectl -n $NS get cluster pg` đủ 2 instance, replica bắt kịp (`kubectl cnpg status pg -n $NS`).

## 4. Failover DB chủ động (bảo trì node, nâng cấp)

```bash
kubectl cnpg promote pg pg-2 -n $NS          # chuyển primary sang pg-2
kubectl -n $NS get cluster pg -w
```

Sau khi chuyển: kiểm tra `/actuator/health` các service là `UP` và log không còn lỗi kết nối DB.

## 5. Keycloak không lên

Ảnh hưởng: khách không đăng nhập hoặc làm mới token được. Token đã cấp vẫn dùng được tới khi hết hạn, vì service kiểm tra chữ ký bằng JWKS đã cache. Trang công khai và giỏ hàng khách vãng lai vẫn chạy.

1. Xem trạng thái:
   ```bash
   kubectl -n $NS get keycloaks.k8s.keycloak.org keycloak -o jsonpath='{.status.conditions}'
   kubectl -n $NS logs statefulset/keycloak --tail=100
   ```
2. Nguyên nhân hay gặp:
   - Không kết nối được `keycloak_db`: kiểm tra `pg-rw`, Secret `pg-keycloak-credentials`, NetworkPolicy `pg`. DB chết thì xem mục 3.
   - Theme jar lỗi sau khi build lại `frontend/keycloak-theme`: quay về image hoặc tag trước.
   - Job import realm (`realm-import-job.yaml`) lỗi do realm JSON sai: xem log job, sửa realm dev rồi chạy lại `node deploy/keycloak/base/realm/build-realm.mjs`.
   - Hết RAM: tăng `resources` trong overlay, hoặc giải phóng node.
3. Operator kẹt: `kubectl -n $NS rollout restart deploy keycloak-operator`.
4. Sau khi lên: thử đăng nhập storefront, admin và gọi một API cần token.

## 6. RabbitMQ / event dồn

- Event không mất khi RabbitMQ chết: bảng `event_publication` của từng service giữ event chưa gửi, gửi lại khi service khởi động lại.
- Message lỗi nằm trong DLQ `<queue>.dlq`. Sau khi sửa nguyên nhân, đẩy lại bằng management UI của RabbitMQ (Shovel hoặc "Move messages" về queue gốc).
- Queue dồn (Grafana / management UI): scale consumer (notification-service) lên, kiểm tra Mailpit/Brevo nếu là email.

## 7. Liên hệ khi sự cố

| Vai trò | Người | Kênh |
|---|---|---|
| Chủ shop (quyết định dừng bán, hoàn tiền) | _điền_ | _điền_ |
| Kỹ thuật trực | _điền_ | _điền_ |
| Nhà cung cấp VPS | _điền_ | _điền_ |

Alert (mục 12) gửi tới kênh ở bảng này. Điền trước go-live.
