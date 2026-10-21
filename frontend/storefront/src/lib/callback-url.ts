/** Chỉ nhận đường dẫn nội bộ để tránh open redirect qua callbackUrl. */
export function safeCallbackUrl(raw: unknown): string {
  return typeof raw === "string" && raw.startsWith("/") && !raw.startsWith("//") ? raw : "/tai-khoan";
}
