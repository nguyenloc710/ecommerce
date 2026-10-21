"use server";

import { redirect } from "next/navigation";
import { KEYCLOAK_ISSUER, signIn, signOut } from "@/auth";
import { safeCallbackUrl } from "@/lib/callback-url";
import { getIdToken } from "@/lib/session";
import { SITE_URL } from "@/lib/site";


export async function login(formData: FormData) {
  await signIn("keycloak", { redirectTo: safeCallbackUrl(formData.get("callbackUrl")) });
}

/** Xóa session Auth.js rồi RP-initiated logout ở Keycloak để đóng luôn phiên SSO. */
export async function logout() {
  const idToken = await getIdToken();
  await signOut({ redirect: false });
  const url = new URL(`${KEYCLOAK_ISSUER}/protocol/openid-connect/logout`);
  url.searchParams.set("post_logout_redirect_uri", new URL("/", SITE_URL).toString());
  if (idToken) url.searchParams.set("id_token_hint", idToken);
  else url.searchParams.set("client_id", process.env.AUTH_KEYCLOAK_ID ?? "storefront-web");
  redirect(url.toString());
}
