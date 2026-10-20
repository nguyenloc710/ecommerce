---
paths:
  - "deploy/**"
  - "infra/**"
  - "local/**"
  - ".github/workflows/**"
---

# Quy tắc cho deploy, hạ tầng và môi trường local

- Chart `deploy/charts/spring-service` dùng chung cho mọi service Spring Boot. Cần thêm tùy chọn thì thêm vào chart với giá trị mặc định giữ nguyên hành vi cũ, không copy chart riêng cho một service.
- `deploy/environments/<env>/<service>.yaml` chỉ chứa khác biệt theo môi trường. `image.tag` do CI cập nhật; không sửa tay trừ khi rollback có chủ đích.
- Secret không bao giờ nằm trong values hay ConfigMap; chỉ qua SealedSecret tên `<service>-secrets`. Tên Service nội bộ cố định: `pg-rw`, `rabbitmq`, `valkey`, `keycloak-service` (xem `deploy/README.md`).
- Mọi Deployment phải có: probes (startup/liveness/readiness), graceful shutdown, `requests` CPU/memory và `limits` memory (không limit CPU), non-root, read-only root filesystem, NetworkPolicy. Chart đã làm sẵn; đừng tắt khi thêm service mới.
- Lệnh có tác động lên cluster thật (`kubectl apply/delete`, `helm upgrade/uninstall`) chỉ chạy khi được yêu cầu rõ ràng; mặc định dùng `helm template` / `kubectl diff` để kiểm tra.
- `local/` chỉ cho dev: mật khẩu và client secret trong đó là giá trị giả, không dùng ở staging/prod. Sửa realm dev (`local/keycloak/ecommerce-realm.json`) thì ghi chú để đưa thay đổi tương ứng vào realm as code ở `deploy/keycloak/` khi phần đó được làm.
- Thay đổi hạ tầng lệch với `docs/ke-hoach-thiet-ke.md` (mục 9–12) thì cập nhật tài liệu cùng PR.
