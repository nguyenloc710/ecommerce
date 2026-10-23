import "server-only";
import { headers } from "next/headers";
import { redirect } from "next/navigation";
import { getToken } from "next-auth/jwt";
import type { Session } from "next-auth";
import { auth, STAFF_ROLES } from "@/auth";
import { ApiError, apiFetch, type ApiFetchInit } from "@/lib/api";

/**
 * Tầng truy cập phiên ở server (Data Access Layer). Token đọc thẳng từ cookie JWT đã mã hóa, không đi qua session
 * trả cho trình duyệt. Proxy (`src/proxy.ts`) đã làm mới token trước khi request tới đây.
 */
async function readToken() {
  const h = await headers();
  return getToken({
    req: { headers: h },
    secret: process.env.AUTH_SECRET,
    // Auth.js dùng cookie tiền tố __Secure- khi chạy https (sau Traefik)
    secureCookie: h.get("x-forwarded-proto") === "https",
  });
}

export async function getAccessToken(): Promise<string | null> {
  const token = await readToken();
  if (!token?.accessToken || token.error) return null;
  if (token.expiresAt && token.expiresAt * 1000 <= Date.now()) return null;
  return token.accessToken;
}

export async function getIdToken(): Promise<string | null> {
  return (await readToken())?.idToken ?? null;
}

/** Phiên hợp lệ hoặc null (chưa đăng nhập, hoặc refresh token đã hết). */
export async function getSession(): Promise<Session | null> {
  const session = await auth();
  return session && !session.error ? session : null;
}

/** Dùng ở trang cần đăng nhập; proxy đã chặn trước, đây là lớp kiểm tra thứ hai ngay tại nơi đọc dữ liệu. */
export async function requireSession(callbackUrl: string): Promise<Session> {
  const session = await getSession();
  if (!session) redirect(`/dang-nhap?callbackUrl=${encodeURIComponent(callbackUrl)}`);
  return session;
}

export async function requireStaff(callbackUrl: string): Promise<Session> {
  const session = await requireSession(callbackUrl);
  if (!isStaff(session)) redirect("/");
  return session;
}

export function isAdmin(session: Pick<Session, "roles"> | null): boolean {
  return !!session?.roles.includes("ADMIN");
}

export function isStaff(session: Pick<Session, "roles"> | null): boolean {
  return !!session?.roles.some((r) => (STAFF_ROLES as readonly string[]).includes(r));
}

/** Gọi gateway kèm access token của người đang đăng nhập. Chỉ dùng ở server. */
export async function authFetch<T>(path: string, init: ApiFetchInit = {}): Promise<T> {
  const token = await getAccessToken();
  if (!token) throw new ApiError(401, "auth.required", "Phiên đăng nhập đã hết, vui lòng đăng nhập lại");
  return apiFetch<T>(path, { ...init, headers: { ...(init.headers ?? {}), Authorization: `Bearer ${token}` } });
}
