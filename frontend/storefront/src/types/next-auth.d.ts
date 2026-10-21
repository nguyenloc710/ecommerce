import type { DefaultSession } from "next-auth";

declare module "next-auth" {
  interface Session {
    user: { id: string } & DefaultSession["user"];
    roles: string[];
    /** "RefreshTokenError": phiên Keycloak đã hết, cần đăng nhập lại. */
    error?: "RefreshTokenError";
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    refreshToken?: string;
    idToken?: string;
    /** Hạn access token, giây epoch. */
    expiresAt?: number;
    roles?: string[];
    error?: "RefreshTokenError";
  }
}
