# keycloak-theme

Theme đăng nhập Keycloak "ecommerce" (E6.4, quyết định B-28): Keycloakify 11 + React, giao diện theo nhận diện storefront,
tiếng Việt mặc định (Keycloak không có sẵn tiếng Việt). Chỉ làm theme **login**: khách quản lý hồ sơ/đơn ở `/tai-khoan`
của storefront nên account console của Keycloak giữ mặc định.

```bash
npm ci
npm run build-keycloak-theme      # → dist_keycloak/keycloak-theme.jar (cần mvn + JDK trên PATH)
```

- Máy dev không có Maven toàn cục: dùng bản wrapper đã tải, ví dụ
  `PATH="$HOME/.m2/wrapper/dists/apache-maven-3.8.4-bin/<hash>/apache-maven-3.8.4/bin:$PATH" npm run build-keycloak-theme`.
- Keycloak local (`local/docker-compose.yml`) mount `dist_keycloak/` vào `/opt/keycloak/providers`. Build lại jar thì
  dừng container Keycloak trước (Windows khóa file đang mount), build xong bật lại.
- Dịch: sửa `scripts/vi-overrides.mjs` rồi `npm run gen-i18n` (sinh `src/login/i18n.vi.ts`, Keycloakify đọc tĩnh file này).
  Câu chưa dịch dùng bản tiếng Anh.
- Style: `src/login/main.css`; class gắn qua bảng `classes` trong `src/login/KcPage.tsx` (CSS PatternFly đã tắt).
- Image cho cluster: `Dockerfile` (Keycloak + jar, `kc.sh build`), dùng trong `deploy/keycloak/base/keycloak.yaml`.
