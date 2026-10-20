---
name: ecom-feature
description: Quy trình chuẩn để thêm hoặc sửa một tính năng backend trong một module Spring Modulith của dự án ecommerce (API, nghiệp vụ, migration, test). Dùng khi người dùng nói "viết API", "thêm tính năng", "làm module X", "thêm endpoint", hoặc giao một việc backend vừa/lớn trong backend/.
argument-hint: "<service>/<module> mô tả tính năng, vd: catalog/product API danh sách sản phẩm"
---

# /ecom-feature: thêm tính năng trong một module

Yêu cầu: $ARGUMENTS

Skill này là lớp đặc thù dự án, đặt trên skill global `java-spring-boot-senior`. Nếu skill đó chưa được nạp trong phiên, nạp nó trước (Skill tool) rồi mới làm theo các bước dưới đây. Quy tắc chi tiết của dự án nằm trong `.claude/rules/backend.md` và tự nạp khi bạn mở file trong `backend/`.

## Bước 0. Định vị
1. Xác định service và module đích (`backend/<service>/src/main/java/vn/ecommerce/<pkg>/<module>/`). Nếu người dùng không nói rõ, suy ra từ bảng module trong `CLAUDE.md`; vẫn mơ hồ thì hỏi một câu.
2. Đọc `docs/ke-hoach-thiet-ke.md` đúng mục của module đó (mục 7) để lấy bảng, luồng và quy tắc đã chốt.
3. Xem module đã có code gì (`Glob` trong package) để nối tiếp "hình dạng" sẵn có, không tạo kiểu mới.

## Bước 1. Chốt hợp đồng (bắt buộc với việc vừa/lớn)
Viết ra 5–10 dòng trước khi code, đưa cho người dùng xem nếu có điểm cần quyết:
- Endpoint: method, path (đúng nhóm `/api`, `/api/admin`, `/internal`), request, response, mã lỗi + message key.
- Quy tắc nghiệp vụ, kể cả biên: rỗng, trùng, đã tồn tại, hết hàng, không có quyền, gọi lại lần hai (idempotency).
- Thay đổi DB: bảng, cột, index, unique, `@Version`.
- Event phát ra / nhận vào (nếu có) → dùng thêm `/ecom-event`.
- Module khác có bị ảnh hưởng không (gọi API public của nó hay không).

## Bước 2. Implement theo thứ tự phụ thuộc
1. **Migration** `V<n>__mo_ta.sql` mới (không sửa file cũ), tương thích ngược, có index cho cột lọc/join.
2. **Entity** đồng bộ tay với SQL (`ddl-auto: validate` sẽ bắt lệch), `@Version` nếu cập nhật đồng thời.
3. **Request/Response DTO** (record), Bean Validation với key `{validation.xxx}`.
4. **Repository**: projection đúng cột cần, `IN` chia lô, không query trong vòng lặp.
5. **Mapper** MapStruct (`unmappedTargetPolicy=ERROR` đã bật ở parent pom).
6. **Service** interface + impl: validate hết → tra dữ liệu → ghi, trong một `@Transactional`; publish event bên trong transaction.
7. **Controller** mỏng, `@Operation` mô tả rõ, lấy user từ `CurrentUser`.
8. **API public của module** (nếu module khác cần gọi): interface + record ở package gốc của module, impl ở package con.
9. **messages.properties** (vi) và **messages_en.properties**: thêm đủ key mới.
10. **Test**: Mockito cho service (mỗi nhánh lỗi một test, verify không ghi khi lỗi); `@WebMvcTest` + `spring-security-test` nếu hợp đồng API quan trọng; `@DataJpaTest` + Testcontainers cho query phức tạp.

## Bước 3. Kiểm tra trước khi báo xong
```bash
cd backend
./mvnw -pl <service> -am verify            # compile main+test, chạy test, ModularityTests
```
- `ModularityTests` fail nghĩa là có gọi chéo trái phép giữa module: sửa kiến trúc, không nới lỏng test.
- Chạy `/ecom-review` trên diff và sửa các mục *phải sửa*.
- Nếu đã làm khác thiết kế (bảng, luồng, tên endpoint): cập nhật `docs/ke-hoach-thiet-ke.md` ngay trong lần thay đổi này.

## Bước 4. Báo cáo
Nêu ngắn gọn: hợp đồng cuối cùng, file đã tạo/sửa, lệnh test đã chạy và kết quả thật, điều gì chưa làm hoặc cần người dùng quyết. Không nói "đã test" khi chưa chạy.
