# Load test

Kiểm tra mục "Load test checkout và đăng nhập ở staging đạt gấp 3 lần tải dự kiến" trong checklist go-live (thiết kế mục 17). Script: `checkout.js` ([k6](https://grafana.com/docs/k6/)).

## Kịch bản

| Kịch bản | Làm gì | Ngưỡng đạt (p95) |
|---|---|---|
| `browse` | Khách vãng lai: danh sách, chi tiết, tồn kho (không token) | < 500 ms |
| `login` | Đăng nhập qua token endpoint Keycloak | < 1.000 ms |
| `checkout` | Khách đã đăng nhập: báo giá, đặt đơn COD, hủy ngay | báo giá < 800 ms, đặt đơn < 1.500 ms |

Chung cho mọi kịch bản: tỷ lệ request lỗi < 1%, check đạt > 99%. Mỗi kịch bản tăng dần trong 1 phút, giữ mức đỉnh `HOLD` (mặc định 3 phút), giảm trong 30 giây.

Đơn tạo ra được hủy ngay để trả hàng về kho. Ghi chú đơn là `LOADTEST - tự hủy`, và báo cáo doanh thu không tính đơn đã hủy.

## Tính tốc độ

Tải dự kiến lấy từ "Số đơn/ngày dự kiến" (thiết kế mục 18, chưa chốt). Giả sử 70% đơn rơi vào 4 giờ cao điểm và cần gấp 3 lần:

```
CHECKOUT_RATE = 3 × (đơn/ngày × 0,7) / (4 × 3600)          # đơn/giây
BROWSE_RATE   = CHECKOUT_RATE × 50                        # ~50 lượt xem mỗi đơn
LOGIN_RATE    = CHECKOUT_RATE × 2
```

Ví dụ: 500 đơn/ngày cho CHECKOUT_RATE ≈ 0,07. k6 cần số nguyên, nên đặt `CHECKOUT_RATE=1` (đã dư nhiều lần). Shop nhỏ thì cứ chạy mặc định (browse 10/s, login 1/s, checkout 1/s) là vượt xa 3 lần tải thật.

## Chuẩn bị ở staging

1. **Tài khoản load test:** tạo 10–20 user trong realm staging, ví dụ `lt01@example.com`… Nên dùng mật khẩu riêng.
2. **Client cho đăng nhập:** tạo client `loadtest` chỉ bật *Direct access grants* (password grant) và gán audience `ecommerce-api` như client `storefront`. **Chỉ tạo ở staging, xóa sau khi chạy.** Prod không được có client password grant.
3. **Tồn kho:** script chỉ đặt SKU giá thường còn ≥ 50 hàng. Đơn bị hủy ngay nên kho không giảm, nhưng cần đủ cho các đơn đang giữ cùng lúc.
4. **Giới hạn tốc độ của gateway:** nếu gateway có bật rate limit theo IP, tạm nâng cho IP máy chạy test, hoặc chạy k6 từ nhiều máy.

## Chạy

```bash
k6 run \
  -e BASE_URL=https://api-staging.example.vn \
  -e KEYCLOAK_TOKEN_URL=https://auth-staging.example.vn/realms/ecommerce/protocol/openid-connect/token \
  -e LOADTEST_CLIENT_ID=loadtest \
  -e LOADTEST_USERS="lt01@example.com:mat-khau,lt02@example.com:mat-khau" \
  -e CHECKOUT_RATE=1 -e BROWSE_RATE=10 -e LOGIN_RATE=1 -e HOLD=5m \
  loadtest/checkout.js
```

Không truyền gì thì script chạy với stack local (`http://localhost:8080`, client `dev-cli`, tài khoản mẫu `customer@example.com`). Không nên chạy local trên máy dev đang thiếu RAM.

## Đọc kết quả

- k6 in bảng tổng kết. Dòng ngưỡng nào có ✗ là không đạt.
- Xem kèm Grafana trong lúc chạy: CPU/RAM node, pool kết nối Hikari, độ trễ PostgreSQL, hàng đợi RabbitMQ (`catalog.order-events`, `notification.events` không được dồn).
- Ghi kết quả (ngày, tốc độ, p95 từng kịch bản, ghi chú tài nguyên) vào `docs/go-live-checklist.md`.
