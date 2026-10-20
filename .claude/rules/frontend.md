---
paths:
  - "frontend/**"
---

# Quy tắc viết code frontend (Next.js 16, App Router)

- **Đọc tài liệu đi kèm phiên bản trước khi viết:** `frontend/storefront/node_modules/next/dist/docs/`. Next.js 16.4 có breaking change so với kiến thức sẵn có; không dựa vào trí nhớ về API cũ.
- Server Components là mặc định. Chỉ thêm `"use client"` khi cần state, effect hoặc event handler, và đặt ở lá nhỏ nhất.
- Trang SEO (trang chủ, danh mục, chi tiết sản phẩm) render trên server và cache (ISR); dữ liệu đọc công khai gọi gateway không cần token.
- Gọi API cần đăng nhập **từ phía server** (Server Components, Route Handlers, Server Actions) với token lấy từ session Auth.js. Token không bao giờ xuất hiện trong JavaScript phía trình duyệt, không lưu `localStorage`.
- Lỗi từ backend có dạng `ErrorResponse { code, message, traceId, errors[] }`: hiển thị `message`, xử lý theo nhánh bằng `code`, cho người dùng copy `traceId` khi báo lỗi.
- Form: React Hook Form + Zod; schema Zod đặt cạnh form, dùng lại cho Server Action.
- Trang quản trị nằm ở `/admin/**` trong cùng app; middleware chặn theo role `STAFF`/`ADMIN` từ session.
- Giao diện tiếng Việt, font `Be Vietnam Pro` đã khai báo trong `layout.tsx`. Tiền hiển thị bằng `Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" })`.
- Ảnh sản phẩm qua `next/image` với domain CDN khai báo trong `next.config.ts`.
- Trước khi báo xong: `npm run lint` và `npm run build` pass.
