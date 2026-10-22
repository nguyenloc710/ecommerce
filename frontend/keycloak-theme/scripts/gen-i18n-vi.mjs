// Sinh src/login/i18n.vi.ts: object literal đầy đủ mọi khóa (Keycloakify phân tích tĩnh file này để tạo
// messages_vi.properties nên không dùng spread/import). Khóa lấy từ bản tiếng Anh của Keycloakify đang cài,
// ghi đè bằng scripts/vi-overrides.mjs. Khóa ghi đè không tồn tại → báo lỗi (gõ sai tên).
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { viOverrides } from "./vi-overrides.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const require = createRequire(path.join(root, "package.json"));
const enModule = require("keycloakify/login/i18n/messages_defaultSet/en.js");
const en = enModule.default ?? enModule;

const unknown = Object.keys(viOverrides).filter((k) => !(k in en));
if (unknown.length) {
  console.error(`Khóa không có trong bản tiếng Anh: ${unknown.join(", ")}`);
  process.exit(1);
}

const merged = { ...en, ...viOverrides };
const lines = Object.entries(merged).map(([k, v]) => `  ${JSON.stringify(k)}: ${JSON.stringify(v)},`);
const out = `// File sinh tự động bởi scripts/gen-i18n-vi.mjs — không sửa tay; sửa scripts/vi-overrides.mjs rồi chạy npm run gen-i18n.
/* eslint-disable */
const messages = {
${lines.join("\n")}
};

export default messages;
`;
fs.writeFileSync(path.join(root, "src/login/i18n.vi.ts"), out);
console.log(`Đã sinh i18n.vi.ts: ${Object.keys(merged).length} khóa, ${Object.keys(viOverrides).length} câu tiếng Việt`);
