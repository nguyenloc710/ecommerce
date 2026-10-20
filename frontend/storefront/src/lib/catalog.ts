import { cacheLife, cacheTag } from "next/cache";
import { ApiError, apiFetch } from "@/lib/api";
import type { ProductCardData } from "@/components/product/product-card";
import { categories as mockCategories, newArrivals as mockNewArrivals } from "@/lib/mock-data";
import { buildListingQuery, type ListingParams } from "@/lib/listing-params";
import {
  CATALOG_CACHE_TAG,
  toCardData,
  type Availability,
  type CategoryTreeNode,
  type FacetsView,
  type PageView,
  type ProductCardView,
  type ProductDetail,
} from "@/lib/catalog-shared";

// Chỉ import file này từ Server Component (dùng next/cache). Client dùng catalog-shared.
export * from "@/lib/catalog-shared";

// ---- Dữ liệu có cache (nằm trong static shell, làm mới theo cacheLife hoặc on-demand) ----

export async function getCategoryTree(): Promise<CategoryTreeNode[]> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG);
  try {
    return await apiFetch<CategoryTreeNode[]>("/api/categories");
  } catch (error) {
    console.warn("[catalog] không lấy được cây danh mục, dùng dữ liệu giả:", (error as Error).message);
    return mockCategoryTree();
  }
}

export async function getNewArrivals(size = 4): Promise<ProductCardData[]> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG);
  try {
    const page = await apiFetch<PageView<ProductCardView>>(`/api/products?sort=newest&size=${size}`);
    return page.items.map(toCardData);
  } catch (error) {
    console.warn("[catalog] không lấy được hàng mới về, dùng dữ liệu giả:", (error as Error).message);
    return mockNewArrivals.slice(0, size);
  }
}

/** Danh sách theo bộ lọc; mỗi tổ hợp tham số là một mục cache riêng. Ném ApiError 404 khi slug danh mục sai. */
export async function listProducts(params: ListingParams): Promise<PageView<ProductCardView>> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG);
  return apiFetch<PageView<ProductCardView>>(`/api/products${buildListingQuery(params)}`);
}

export async function getFacets(category?: string, brand?: string, q?: string): Promise<FacetsView> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG);
  const qs = new URLSearchParams();
  if (category) qs.set("category", category);
  if (brand) qs.set("brand", brand);
  if (q) qs.set("q", q);
  const query = qs.toString();
  try {
    return await apiFetch<FacetsView>(`/api/products/facets${query ? `?${query}` : ""}`);
  } catch (error) {
    console.warn("[catalog] không lấy được facet:", (error as Error).message);
    return { options: [], attributes: [], priceMin: null, priceMax: null };
  }
}

/** Chi tiết sản phẩm; null khi không tồn tại hoặc ngừng bán (để trang gọi notFound()). */
export async function getProduct(slug: string): Promise<ProductDetail | null> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG, `product:${slug}`);
  try {
    return await apiFetch<ProductDetail>(`/api/products/${encodeURIComponent(slug)}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

/** Sản phẩm cùng danh mục cho khối "Hoàn thiện bộ". */
export async function getRelatedProducts(categorySlug: string, excludeSlug: string, size = 4): Promise<ProductCardData[]> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG);
  try {
    const page = await apiFetch<PageView<ProductCardView>>(`/api/products?category=${encodeURIComponent(categorySlug)}&size=${size + 1}`);
    return page.items.filter((p) => p.slug !== excludeSlug).slice(0, size).map(toCardData);
  } catch (error) {
    console.warn("[catalog] không lấy được sản phẩm liên quan:", (error as Error).message);
    return [];
  }
}

/** Tồn kho hiện tại: KHÔNG cache, đọc lúc request (gọi trong Suspense). */
export async function getAvailability(skuIds: string[]): Promise<Record<string, Availability>> {
  if (skuIds.length === 0) return {};
  try {
    // Timeout dài hơn mặc định: lần gọi đầu sau khi backend khởi động có thể chậm (JIT), mà đây là dữ liệu không cache
    const list = await apiFetch<Availability[]>(`/api/inventory/availability?skuIds=${skuIds.join(",")}`, { cache: "no-store", timeoutMs: 10_000 });
    return Object.fromEntries(list.map((a) => [a.skuId, a]));
  } catch (error) {
    console.warn("[catalog] không lấy được tồn kho:", (error as Error).message);
    return {};
  }
}

function mockCategoryTree(): CategoryTreeNode[] {
  return [
    {
      id: "mock-root",
      name: "Tất cả",
      slug: "tat-ca",
      sortOrder: 0,
      children: mockCategories.map((c, i) => ({ id: `mock-${i}`, name: c.name, slug: c.slug, sortOrder: i, children: [] })),
    },
  ];
}
