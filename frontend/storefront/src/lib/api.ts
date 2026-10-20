/**
 * Gọi gateway từ phía server. Ưu tiên API_BASE_URL (địa chỉ nội bộ trong cluster), không có thì dùng
 * NEXT_PUBLIC_API_BASE_URL (địa chỉ public, dùng cả ở trình duyệt).
 */
const BASE_URL = process.env.API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const DEFAULT_TIMEOUT_MS = 5000;

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string | undefined,
    message: string,
    public readonly traceId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type ErrorBody = { code?: string; message?: string; traceId?: string };

export async function apiFetch<T>(path: string, init: RequestInit & { timeoutMs?: number } = {}): Promise<T> {
  const { timeoutMs = DEFAULT_TIMEOUT_MS, ...rest } = init;
  const res = await fetch(`${BASE_URL}${path}`, {
    ...rest,
    headers: { Accept: "application/json", "Accept-Language": "vi", ...(rest.headers ?? {}) },
    signal: AbortSignal.timeout(timeoutMs),
  });
  if (!res.ok) {
    let body: ErrorBody = {};
    try {
      body = (await res.json()) as ErrorBody;
    } catch {
      // body không phải JSON (ví dụ 502 từ proxy): giữ message mặc định
    }
    throw new ApiError(res.status, body.code, body.message ?? `Lỗi ${res.status} khi gọi ${path}`, body.traceId);
  }
  return (await res.json()) as T;
}
