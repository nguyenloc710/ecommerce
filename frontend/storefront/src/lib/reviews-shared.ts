/** Kiểu dữ liệu đánh giá theo hợp đồng catalog-service (module review), dùng chung server/client. */

export type ReviewItem = {
  id: string;
  rating: number;
  content: string | null;
  authorName: string;
  variantLabel: string | null;
  imageUrls: string[];
  createdAt: string;
};

export type ProductReviews = {
  productId: string;
  averageRating: number;
  totalCount: number;
  countByRating: Record<string, number>;
  reviews: { items: ReviewItem[]; page: number; size: number; total: number; hasNext: boolean };
};

export type EligibilityReason = "LOGIN_REQUIRED" | "NOT_PURCHASED" | "ALREADY_REVIEWED" | "OK";
export type Eligibility = { canReview: boolean; reason: EligibilityReason };

export type UploadTicket = { uploadUrl: string; objectKey: string; publicUrl: string; headers: Record<string, string>; expiresAt: string };

/** Khớp ecommerce.review.max-images và ecommerce.media.* của catalog-service (server vẫn kiểm tra lại). */
export const REVIEW_MAX_IMAGES = 5;
export const REVIEW_MAX_IMAGE_BYTES = 5 * 1024 * 1024;
export const REVIEW_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
export const REVIEW_MAX_CONTENT = 2000;

export const EMPTY_REVIEWS = (productId: string): ProductReviews => ({
  productId,
  averageRating: 0,
  totalCount: 0,
  countByRating: { "5": 0, "4": 0, "3": 0, "2": 0, "1": 0 },
  reviews: { items: [], page: 0, size: 10, total: 0, hasNext: false },
});
