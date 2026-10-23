// Load test checkout + đăng nhập (checklist go-live mục 17: "đạt gấp 3 lần tải dự kiến"). Chạy với k6:
//   k6 run -e BASE_URL=https://shop-staging.example.vn/gw -e KEYCLOAK_TOKEN_URL=... -e LOADTEST_USERS=lt1@example.com:pw,... loadtest/checkout.js
// Xem loadtest/README.md: cách tính tốc độ, chuẩn bị tài khoản/tồn kho, cách đọc kết quả.
//
// Ba kịch bản chạy song song:
//   browse   – khách vãng lai xem danh sách, chi tiết, tồn kho (không token)
//   login    – đăng nhập qua token endpoint Keycloak (password grant của client chỉ dùng cho load test)
//   checkout – khách đã đăng nhập: báo giá → đặt đơn COD → hủy ngay (nhả hàng, không làm bẩn số liệu)
import http from "k6/http";
import { check, fail } from "k6";
import { Trend } from "k6/metrics";
import exec from "k6/execution";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const TOKEN_URL = __ENV.KEYCLOAK_TOKEN_URL || "http://localhost:8180/realms/ecommerce/protocol/openid-connect/token";
const CLIENT_ID = __ENV.LOADTEST_CLIENT_ID || "dev-cli";
const USERS = (__ENV.LOADTEST_USERS || "customer@example.com:Customer123").split(",").map((u) => {
  const i = u.indexOf(":");
  return { username: u.slice(0, i), password: u.slice(i + 1) };
});
// Tốc độ mỗi giây ở mức đỉnh; đặt bằng 3 × tải dự kiến (README)
const BROWSE_RATE = Number(__ENV.BROWSE_RATE || 10);
const LOGIN_RATE = Number(__ENV.LOGIN_RATE || 1);
const CHECKOUT_RATE = Number(__ENV.CHECKOUT_RATE || 1);
const HOLD = __ENV.HOLD || "3m";
// Địa chỉ giao hàng hợp lệ theo bảng hành chính (B-13); mặc định Ba Đình, Hà Nội
const PROVINCE = __ENV.PROVINCE_CODE || "01";
const WARD = __ENV.WARD_CODE || "00004";

const quoteTime = new Trend("checkout_quote_ms", true);
const placeTime = new Trend("checkout_place_ms", true);
const loginTime = new Trend("login_ms", true);

const ramp = (rate) => [
  { target: rate, duration: "1m" },
  { target: rate, duration: HOLD },
  { target: 0, duration: "30s" },
];

export const options = {
  scenarios: {
    browse: { executor: "ramping-arrival-rate", exec: "browse", startRate: 0, timeUnit: "1s", preAllocatedVUs: 20, maxVUs: 200, stages: ramp(BROWSE_RATE) },
    login: { executor: "ramping-arrival-rate", exec: "login", startRate: 0, timeUnit: "1s", preAllocatedVUs: 5, maxVUs: 50, stages: ramp(LOGIN_RATE) },
    checkout: { executor: "ramping-arrival-rate", exec: "checkout", startRate: 0, timeUnit: "1s", preAllocatedVUs: 10, maxVUs: 100, stages: ramp(CHECKOUT_RATE) },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{scenario:browse}": ["p(95)<500"],
    checkout_quote_ms: ["p(95)<800"],
    checkout_place_ms: ["p(95)<1500"],
    login_ms: ["p(95)<1000"],
    checks: ["rate>0.99"],
  },
};

function token(user) {
  const res = http.post(TOKEN_URL, { grant_type: "password", client_id: CLIENT_ID, username: user.username, password: user.password }, { tags: { name: "keycloak token" } });
  if (res.status !== 200) return null;
  return res.json();
}

/** Chuẩn bị: lấy danh sách SKU còn nhiều hàng để đặt, kiểm tra đăng nhập được với tài khoản load test. */
export function setup() {
  const products = http.get(`${BASE_URL}/api/products?size=48&sort=newest`).json("items") || [];
  const skus = [];
  for (const p of products) {
    const detail = http.get(`${BASE_URL}/api/products/${p.slug}`);
    if (detail.status !== 200) continue;
    const variants = detail.json("variants");
    const stock = http.get(`${BASE_URL}/api/inventory/availability?skuIds=${variants.map((v) => v.id).join(",")}`).json() || [];
    for (const v of variants) {
      const a = stock.find((s) => s.skuId === v.id);
      // Chỉ lấy SKU giá thường còn nhiều hàng: đơn bị hủy ngay nên tồn kho không giảm, nhưng cần đủ cho đơn đang giữ cùng lúc
      if (a && a.inStock && (a.remaining === null || a.remaining >= 50) && !v.priceLabel) skus.push({ skuId: v.id, slug: p.slug });
    }
  }
  if (skus.length === 0) fail("Không có SKU nào còn ≥ 50 hàng để đặt; nhập thêm tồn kho cho môi trường test (README)");
  if (!token(USERS[0])) fail(`Không đăng nhập được ${USERS[0].username}; kiểm tra LOADTEST_USERS và client ${CLIENT_ID}`);
  return { skus, slugs: [...new Set(skus.map((s) => s.slug))] };
}

export function browse(data) {
  const slug = data.slugs[Math.floor(Math.random() * data.slugs.length)];
  const list = http.get(`${BASE_URL}/api/products?size=24&sort=newest`, { tags: { name: "GET /api/products" } });
  const detail = http.get(`${BASE_URL}/api/products/${slug}`, { tags: { name: "GET /api/products/{slug}" } });
  check(list, { "danh sách 200": (r) => r.status === 200 });
  if (!check(detail, { "chi tiết 200": (r) => r.status === 200 })) return;
  const ids = detail.json("variants").map((v) => v.id).join(",");
  check(http.get(`${BASE_URL}/api/inventory/availability?skuIds=${ids}`, { tags: { name: "GET /api/inventory/availability" } }), {
    "tồn kho 200": (r) => r.status === 200,
  });
}

export function login() {
  const user = USERS[exec.scenario.iterationInTest % USERS.length];
  const started = Date.now();
  const t = token(user);
  loginTime.add(Date.now() - started);
  check(t, { "đăng nhập được": (v) => v !== null && !!v.access_token });
}

// Token theo VU, đăng nhập lại khi sắp hết hạn (access token Keycloak sống ngắn)
const session = { access: null, expiresAt: 0 };

function authHeaders() {
  if (!session.access || Date.now() > session.expiresAt - 30_000) {
    const user = USERS[(exec.vu.idInTest - 1) % USERS.length];
    const t = token(user);
    if (!t) return null;
    session.access = t.access_token;
    session.expiresAt = Date.now() + t.expires_in * 1000;
  }
  return { Authorization: `Bearer ${session.access}`, "Content-Type": "application/json" };
}

function uuid() {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
  });
}

export function checkout(data) {
  const headers = authHeaders();
  if (!check(headers, { "có token": (h) => h !== null })) return;
  const sku = data.skus[Math.floor(Math.random() * data.skus.length)];
  const body = JSON.stringify({
    lines: [{ skuId: sku.skuId, quantity: 1 }],
    address: { recipientName: "Load Test", phone: "0900000000", provinceCode: PROVINCE, provinceName: "Load test", wardCode: WARD, wardName: "Load test", line1: "1 Load test" },
    paymentMethod: "COD",
    note: "LOADTEST - tự hủy",
  });

  const quote = http.post(`${BASE_URL}/api/checkout/quote`, body, { headers, tags: { name: "POST /api/checkout/quote" } });
  quoteTime.add(quote.timings.duration);
  if (!check(quote, { "báo giá 200": (r) => r.status === 200 })) return;

  const placed = http.post(`${BASE_URL}/api/orders`, body, { headers: { ...headers, "Idempotency-Key": uuid() }, tags: { name: "POST /api/orders" } });
  placeTime.add(placed.timings.duration);
  if (!check(placed, { "đặt đơn 201": (r) => r.status === 201 })) return;

  const code = placed.json("order.code");
  check(http.post(`${BASE_URL}/api/orders/${code}/cancel`, JSON.stringify({ reason: "LOADTEST" }), { headers, tags: { name: "POST /api/orders/{code}/cancel" } }), {
    "hủy đơn 200": (r) => r.status === 200,
  });
}
