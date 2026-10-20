## Mục đích

<!-- Làm gì, vì sao. Link issue nếu có. -->

## Thay đổi chính

-

## Checklist

- [ ] `./mvnw verify` pass (gồm `ModularityTests`) / `npm run build` pass với frontend
- [ ] Migration chỉ **thêm file mới**, tương thích ngược (không sửa file `V*.sql` đã merge)
- [ ] Lỗi trả về qua `ApiException` với message key; đã thêm key vào `messages.properties` (vi) và `_en`
- [ ] Không log token, mật khẩu, PII chưa che
- [ ] Endpoint mới đúng nhóm quyền: `/api/**` (khách), `/api/admin/**` (STAFF/ADMIN), `/internal/**` (INTERNAL)
- [ ] Nếu lệch thiết kế: đã cập nhật `docs/ke-hoach-thiet-ke.md` trong PR này
- [ ] Không có secret thật trong diff

## Cách test

<!-- Lệnh / bước đã chạy để kiểm tra. -->
