# storefront

Next.js 16 (App Router, TypeScript, Tailwind v4). Gồm cả trang quản trị `/admin`.

## Chạy

```bash
npm install
npm run dev      # http://localhost:3000
npm run lint
npm run build    # output standalone (.next/standalone) để đóng image
```

## Biến môi trường

Tạo `.env.local` (không commit):

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `NEXT_PUBLIC_SHOP_NAME` | `Tên shop` | Tên hiển thị ở logo, title, footer |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | Gateway backend |

Biến cho Auth.js (Keycloak) sẽ thêm ở task E5.1.

## Cấu trúc

```
src/app/                 route (App Router)
src/components/site/     header, footer
src/components/product/  thẻ sản phẩm
src/components/ui/       thành phần dùng chung (placeholder ảnh...)
src/lib/                 format tiền, dữ liệu giả tạm thời
```

Token thiết kế (màu, font, nút, eyebrow) khai báo trong `src/app/globals.css`, giá trị chốt ở `docs/decisions.md` mục UI-*.
Trước khi viết code đọc `node_modules/next/dist/docs/` vì Next.js 16.4 có thay đổi so với các bản trước.
