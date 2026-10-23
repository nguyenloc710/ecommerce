"use server";

import { ApiError } from "@/lib/api";
import { REVIEW_MAX_CONTENT, REVIEW_MAX_IMAGES, type Eligibility, type UploadTicket } from "@/lib/reviews-shared";
import { authFetch, getAccessToken } from "@/lib/session";

/** Server Action cho đánh giá: token Keycloak chỉ dùng ở server (BFF). Lỗi trả dạng giá trị kèm traceId. */
export type ReviewActionError = { ok: false; message: string; code?: string; traceId?: string };

const JSON_HEADERS = { "Content-Type": "application/json" };

function toError(error: unknown): ReviewActionError {
  if (error instanceof ApiError) return { ok: false, message: error.message, code: error.code, traceId: error.traceId };
  console.warn("[reviews] lỗi không mong đợi:", (error as Error).message);
  return { ok: false, message: "Không kết nối được hệ thống, vui lòng thử lại." };
}

export async function getReviewEligibility(productId: string): Promise<Eligibility> {
  if (!(await getAccessToken())) return { canReview: false, reason: "LOGIN_REQUIRED" };
  try {
    return await authFetch<Eligibility>(`/api/reviews/eligibility?productId=${encodeURIComponent(productId)}`, { cache: "no-store" });
  } catch {
    return { canReview: false, reason: "LOGIN_REQUIRED" };
  }
}

/** Xin URL ký sẵn; trình duyệt PUT ảnh thẳng lên kho ảnh (không đi qua storefront). */
export async function requestReviewUpload(contentType: string, size: number): Promise<({ ok: true } & UploadTicket) | ReviewActionError> {
  try {
    const ticket = await authFetch<UploadTicket>("/api/media/review-images", {
      method: "POST",
      headers: JSON_HEADERS,
      body: JSON.stringify({ contentType, size }),
    });
    return { ok: true, ...ticket };
  } catch (error) {
    return toError(error);
  }
}

export async function submitReview(input: {
  productId: string;
  rating: number;
  content: string;
  imageKeys: string[];
}): Promise<{ ok: true } | ReviewActionError> {
  const rating = Math.trunc(Number(input.rating));
  if (rating < 1 || rating > 5) return { ok: false, message: "Vui lòng chọn số sao." };
  try {
    await authFetch("/api/reviews", {
      method: "POST",
      headers: JSON_HEADERS,
      body: JSON.stringify({
        productId: input.productId,
        rating,
        content: input.content.trim().slice(0, REVIEW_MAX_CONTENT) || undefined,
        imageKeys: input.imageKeys.slice(0, REVIEW_MAX_IMAGES),
      }),
    });
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}
