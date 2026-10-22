// Sinh realm as code cho staging/prod từ realm dev (local/keycloak/ecommerce-realm.json).
// Chạy lại mỗi khi sửa realm dev:  node deploy/keycloak/base/realm/build-realm.mjs
//
// - Bỏ client dev-cli (P-08) và user thử nghiệm; giữ user service account (mang role INTERNAL, realm-management).
// - Secret, URL storefront, SMTP thay bằng biến $(env:...) để keycloak-config-cli điền lúc áp dụng
//   (IMPORT_VARSUBSTITUTION_ENABLED=true, giá trị từ Secret keycloak-realm-secrets).
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const source = path.resolve(here, "../../../../local/keycloak/ecommerce-realm.json");
const target = path.join(here, "ecommerce-realm.json");

const realm = JSON.parse(fs.readFileSync(source, "utf8"));

const DEV_ONLY_CLIENTS = new Set(["dev-cli"]);
const CLIENT_SECRETS = {
  "storefront-web": "$(env:STOREFRONT_CLIENT_SECRET)",
  "order-service": "$(env:ORDER_SERVICE_CLIENT_SECRET)",
  "customer-service": "$(env:CUSTOMER_SERVICE_CLIENT_SECRET)",
};
const STOREFRONT_URL = "$(env:STOREFRONT_URL)";

realm.sslRequired = "external";
realm.clients = realm.clients
  .filter((c) => !DEV_ONLY_CLIENTS.has(c.clientId))
  .map((c) => {
    const out = { ...c };
    if (CLIENT_SECRETS[c.clientId]) out.secret = CLIENT_SECRETS[c.clientId];
    if (c.clientId === "storefront-web") {
      out.redirectUris = [`${STOREFRONT_URL}/*`];
      out.webOrigins = [STOREFRONT_URL];
      out.attributes = { ...c.attributes, "post.logout.redirect.uris": `${STOREFRONT_URL}/*` };
    }
    return out;
  });
const missing = Object.keys(CLIENT_SECRETS).filter((id) => !realm.clients.some((c) => c.clientId === id));
if (missing.length) throw new Error(`Realm dev thiếu client: ${missing.join(", ")}`);

realm.users = (realm.users ?? []).filter((u) => u.serviceAccountClientId || u.username.startsWith("service-account-"));

// SMTP thật (Brevo, thiết kế 9.3); Keycloak gửi email xác minh/quên mật khẩu
realm.smtpServer = {
  host: "$(env:SMTP_HOST)",
  port: "$(env:SMTP_PORT)",
  from: "$(env:SMTP_FROM)",
  fromDisplayName: "$(env:SMTP_FROM_NAME)",
  auth: "true",
  user: "$(env:SMTP_USERNAME)",
  password: "$(env:SMTP_PASSWORD)",
  starttls: "true",
};

fs.writeFileSync(target, JSON.stringify(realm, null, 2) + "\n");
console.log(`Đã ghi ${path.relative(process.cwd(), target)}: ${realm.clients.length} client, ${realm.users.length} service account`);
