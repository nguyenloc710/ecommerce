import { NextResponse, type NextRequest } from "next/server";
import { auth, STAFF_ROLES } from "@/auth";
import { categoryExists, productExists } from "@/lib/seo-guard";

/** Trang bắt buộc đăng nhập. Đây là kiểm tra lạc quan; trang vẫn gọi requireSession/requireStaff ở server. */
const PROTECTED_PREFIXES = ["/tai-khoan", "/thanh-toan", "/admin"];

const startsWithAny = (path: string, prefixes: string[]) => prefixes.some((p) => path === p || path.startsWith(`${p}/`));

/**
 * `auth(...)` chạy callback `jwt` (làm mới access token khi sắp hết hạn) và trả cookie session mới qua Set-Cookie.
 * Cookie mới chỉ tới trình duyệt ở response, nên chép thêm vào header Cookie của request hiện tại để Server Component
 * phía sau đọc được token mới. Refresh token xoay vòng (revokeRefreshToken) nên không được để server làm mới lần hai.
 */
const withSession = auth((req) => {
  const { pathname, search } = req.nextUrl;
  if (!startsWithAny(pathname, PROTECTED_PREFIXES)) return;

  const session = req.auth && !req.auth.error ? req.auth : null;
  if (!session) {
    const login = new URL("/dang-nhap", req.nextUrl);
    login.searchParams.set("callbackUrl", `${pathname}${search}`);
    return NextResponse.redirect(login);
  }
  if (startsWithAny(pathname, ["/admin"]) && !session.roles.some((r) => (STAFF_ROLES as readonly string[]).includes(r))) {
    return NextResponse.redirect(new URL("/", req.nextUrl));
  }
});

/** /san-pham/{slug}, /danh-muc/{slug} không tồn tại → rewrite sang đường dẫn không có route để nhận 404 thật. */
const SEO_ROUTES: [RegExp, (slug: string) => Promise<boolean>][] = [
  [/^\/san-pham\/([^/]+)$/, productExists],
  [/^\/danh-muc\/([^/]+)$/, categoryExists],
];
const NOT_FOUND_PATH = "/__khong-ton-tai";

export async function proxy(req: NextRequest) {
  if (req.method === "GET") {
    for (const [pattern, exists] of SEO_ROUTES) {
      const slug = pattern.exec(req.nextUrl.pathname)?.[1];
      if (slug && !(await exists(decodeURIComponent(slug)))) return NextResponse.rewrite(new URL(NOT_FOUND_PATH, req.nextUrl));
    }
  }

  const res = (await withSession(req, { params: Promise.resolve({}) })) as Response;
  if (res.headers.has("location")) return res;

  const setCookies = res.headers.getSetCookie();
  if (setCookies.length === 0) return NextResponse.next();

  const headers = new Headers(req.headers);
  headers.set("cookie", mergeCookies(req.headers.get("cookie"), setCookies));
  const next = NextResponse.next({ request: { headers } });
  for (const c of setCookies) next.headers.append("set-cookie", c);
  return next;
}

function mergeCookies(cookieHeader: string | null, setCookies: string[]): string {
  const jar = new Map<string, string>();
  for (const part of (cookieHeader ?? "").split(";")) {
    const i = part.indexOf("=");
    if (i > 0) jar.set(part.slice(0, i).trim(), part.slice(i + 1).trim());
  }
  for (const sc of setCookies) {
    const [pair, ...attrs] = sc.split(";");
    const i = pair.indexOf("=");
    if (i <= 0) continue;
    const name = pair.slice(0, i).trim();
    const deleted = attrs.some((a) => {
      const [k, v = ""] = a.trim().split("=");
      const key = k.toLowerCase();
      return (key === "max-age" && Number(v) <= 0) || (key === "expires" && Date.parse(v) <= Date.now());
    });
    if (deleted) jar.delete(name);
    else jar.set(name, pair.slice(i + 1).trim());
  }
  return [...jar].map(([k, v]) => `${k}=${v}`).join("; ");
}

export const config = {
  matcher: ["/((?!api/auth|api/health|_next/static|_next/image|favicon.ico|.*\.(?:png|jpg|jpeg|svg|webp|avif|ico)$).*)"],
};
