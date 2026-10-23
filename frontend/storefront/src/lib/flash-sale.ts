import { cacheLife, cacheTag } from "next/cache";
import { apiFetch } from "@/lib/api";
import { CATALOG_CACHE_TAG } from "@/lib/catalog-shared";
import type { CurrentFlashSale } from "@/lib/flash-sale-shared";

/**
 * Flash sale đang chạy (công khai). Cache theo phút như danh sách sản phẩm; admin tạo/dừng chương trình thì
 * updateTag(catalog). Mất kết nối → không hiện khối flash sale.
 */
export async function getCurrentFlashSales(): Promise<CurrentFlashSale[]> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG, "flash-sales");
  try {
    return await apiFetch<CurrentFlashSale[]>("/api/flash-sales/current");
  } catch (error) {
    console.warn("[flash-sale] không lấy được flash sale đang chạy:", (error as Error).message);
    return [];
  }
}
