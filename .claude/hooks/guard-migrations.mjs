#!/usr/bin/env node
// Hook PreToolUse cho Edit/Write: chặn sửa file Flyway migration đã commit.
// Migration đã merge là bất biến; muốn đổi schema thì tạo file V<n+1>__*.sql mới.
// Viết bằng Node để chạy giống nhau trên Windows, macOS, Linux và CI.

import { execFileSync } from "node:child_process";
import path from "node:path";

const MIGRATION_PATTERN = /[\\/]db[\\/]migration[\\/]V\d+__.+\.sql$/i;

function readStdin() {
  return new Promise((resolve) => {
    let data = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => (data += chunk));
    process.stdin.on("end", () => resolve(data));
  });
}

function isTrackedInGit(filePath, projectDir) {
  try {
    execFileSync("git", ["ls-files", "--error-unmatch", "--", filePath], {
      cwd: projectDir,
      stdio: ["ignore", "ignore", "ignore"],
    });
    return true;
  } catch {
    return false;
  }
}

function deny(reason) {
  process.stdout.write(
    JSON.stringify({
      hookSpecificOutput: {
        hookEventName: "PreToolUse",
        permissionDecision: "deny",
        permissionDecisionReason: reason,
      },
    }),
  );
  process.exit(0);
}

const raw = await readStdin();
let input;
try {
  input = JSON.parse(raw);
} catch {
  process.exit(0); // không parse được thì không chặn
}

const filePath = input?.tool_input?.file_path;
if (!filePath || !MIGRATION_PATTERN.test(filePath)) {
  process.exit(0);
}

const projectDir = process.env.CLAUDE_PROJECT_DIR || process.cwd();
// Git Bash có thể đưa đường dẫn dạng /d/LOC/... ; đổi về D:/LOC/... để path/git của Windows hiểu
const normalized =
  process.platform === "win32" ? filePath.replace(/^\/([a-zA-Z])\//, (_, drive) => `${drive.toUpperCase()}:/`) : filePath;
const absolute = path.isAbsolute(normalized) ? normalized : path.join(projectDir, normalized);

if (isTrackedInGit(absolute, projectDir)) {
  deny(
    `Không được sửa migration đã commit: ${path.relative(projectDir, absolute)}. ` +
      "Tạo file Flyway mới (V<n+1>__mo_ta.sql) với thay đổi tương thích ngược.",
  );
}
