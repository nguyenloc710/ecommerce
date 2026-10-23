import { cacheLife, cacheTag } from "next/cache";
import { apiFetch } from "@/lib/api";
import { CATALOG_CACHE_TAG } from "@/lib/catalog-shared";
import { EMPTY_REVIEWS, type ProductReviews } from "@/lib/reviews-shared";

/**
 * Đánh giá đã duyệt của sản phẩm (công khai). Cache theo phút, nằm trong phần prerender của trang sản phẩm;
 * admin duyệt/ẩn đánh giá thì updateTag(catalog) để trang thấy ngay. Mất kết nối → rỗng, tự làm mới sau cacheLife.
 */
export async function getProductReviews(productId: string, page = 0): Promise<ProductReviews> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG, `reviews:${productId}`);
  try {
    return await apiFetch<ProductReviews>(`/api/reviews?productId=${encodeURIComponent(productId)}&page=${page}`);
  } catch (error) {
    console.warn("[reviews] không lấy được đánh giá:", (error as Error).message);
    return EMPTY_REVIEWS(productId);
  }
}
