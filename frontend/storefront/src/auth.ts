import NextAuth from "next-auth";
import type { JWT } from "next-auth/jwt";
import Keycloak from "next-auth/providers/keycloak";

/**
 * Auth.js theo mô hình BFF (mục 6 thiết kế): token Keycloak chỉ nằm trong JWT mã hóa ở cookie httpOnly.
 * Session trả cho trình duyệt chỉ có thông tin hiển thị + role; lấy access token ở server qua `getAccessToken()`.
 */
export const KEYCLOAK_ISSUER = process.env.AUTH_KEYCLOAK_ISSUER ?? "http://localhost:8180/realms/ecommerce";
const CLIENT_ID = process.env.AUTH_KEYCLOAK_ID ?? "storefront-web";

/** Làm mới sớm hơn hạn thật để request đang chạy không cầm token vừa hết hạn. */
const REFRESH_SKEW_SECONDS = 60;
/** Bằng SSO Session Max của realm (10 giờ); hết hạn thì refresh token cũng hết, buộc đăng nhập lại. */
const SESSION_MAX_AGE_SECONDS = 10 * 60 * 60;

export const STAFF_ROLES = ["STAFF", "ADMIN"] as const;

/** Đọc claim phẳng `roles` (mapper realm-roles-flat). Chỉ decode để hiển thị/điều hướng; backend vẫn tự kiểm tra chữ ký. */
function rolesOf(accessToken: string): string[] {
  try {
    const payload = JSON.parse(Buffer.from(accessToken.split(".")[1], "base64url").toString("utf8")) as { roles?: unknown };
    return Array.isArray(payload.roles) ? payload.roles.filter((r): r is string => typeof r === "string") : [];
  } catch {
    return [];
  }
}

type TokenResponse = { access_token: string; expires_in: number; refresh_token?: string; id_token?: string };

async function refreshAccessToken(token: JWT): Promise<JWT> {
  if (!token.refreshToken) return { ...token, error: "RefreshTokenError" };
  try {
    const res = await fetch(`${KEYCLOAK_ISSUER}/protocol/openid-connect/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        client_id: CLIENT_ID,
        client_secret: process.env.AUTH_KEYCLOAK_SECRET ?? "",
        refresh_token: token.refreshToken,
      }),
      signal: AbortSignal.timeout(5000),
    });
    if (!res.ok) {
      // 400 invalid_grant: phiên Keycloak đã hết/bị khóa tài khoản. Không log body (có thể chứa token)
      console.warn(`[auth] làm mới token thất bại, status=${res.status}`);
      return { ...token, error: "RefreshTokenError" };
    }
    const data = (await res.json()) as TokenResponse;
    return {
      ...token,
      accessToken: data.access_token,
      expiresAt: Math.floor(Date.now() / 1000) + data.expires_in,
      // Realm bật revokeRefreshToken: refresh token xoay vòng, phải lưu cái mới
      refreshToken: data.refresh_token ?? token.refreshToken,
      idToken: data.id_token ?? token.idToken,
      roles: rolesOf(data.access_token),
      error: undefined,
    };
  } catch (e) {
    console.warn(`[auth] không gọi được Keycloak để làm mới token: ${(e as Error).message}`);
    return { ...token, error: "RefreshTokenError" };
  }
}

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    Keycloak({
      issuer: KEYCLOAK_ISSUER,
      clientId: CLIENT_ID,
      clientSecret: process.env.AUTH_KEYCLOAK_SECRET,
    }),
  ],
  session: { strategy: "jwt", maxAge: SESSION_MAX_AGE_SECONDS },
  pages: { signIn: "/dang-nhap", error: "/dang-nhap" },
  callbacks: {
    async jwt({ token, account }) {
      if (account?.access_token) {
        return {
          ...token,
          accessToken: account.access_token,
          refreshToken: account.refresh_token,
          idToken: account.id_token,
          expiresAt: account.expires_at ?? Math.floor(Date.now() / 1000) + 300,
          roles: rolesOf(account.access_token),
        };
      }
      if (token.error || !token.expiresAt) return token;
      if (Date.now() / 1000 < token.expiresAt - REFRESH_SKEW_SECONDS) return token;
      return refreshAccessToken(token);
    },
    session({ session, token }) {
      // Không đưa accessToken/refreshToken vào đây: object này trả nguyên cho trình duyệt qua /api/auth/session
      session.user.id = token.sub ?? "";
      session.roles = token.roles ?? [];
      session.error = token.error;
      return session;
    },
  },
});
