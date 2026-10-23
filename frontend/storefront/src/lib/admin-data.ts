import "server-only";
import { notFound } from "next/navigation";
import { ApiError } from "@/lib/api";
import type { AdminOrderRow, AdminReviewRow, BrandOption, CategoryOption, GoldRateView, InventoryRow, Paged, ProductAdmin, ProductAdminRow } from "@/lib/admin-shared";
import type { OrderStatus, OrderView } from "@/lib/checkout-shared";
import type { AdminFlashSaleDetail, AdminFlashSaleRow } from "@/lib/flash-sale-shared";
import { authFetch } from "@/lib/session";

/** Đọc dữ liệu quản trị (không cache: nhân viên cần thấy số liệu mới nhất). */
const get = <T>(path: string) => authFetch<T>(path, { cache: "no-store" });

const qs = (params: Record<string, string | number | boolean | undefined>) => {
  const p = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== "" && v !== false) p.set(k, String(v));
  const s = p.toString();
  return s ? `?${s}` : "";
};

async function orNotFound<T>(promise: Promise<T>): Promise<T> {
  try {
    return await promise;
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  }
}

export const listAdminOrders = (o: { status?: OrderStatus; q?: string; page: number; size?: number }) =>
  get<Paged<AdminOrderRow>>(`/api/admin/orders${qs({ status: o.status, q: o.q, page: o.page, size: o.size ?? 20 })}`);

export const getAdminOrder = (code: string) => orNotFound(get<OrderView>(`/api/admin/orders/${encodeURIComponent(code)}`));

export const listAdminProducts = (o: { q?: string; status?: string; page: number }) =>
  get<Paged<ProductAdminRow>>(`/api/admin/catalog/products${qs({ q: o.q, status: o.status, page: o.page, size: 20 })}`);

export const getAdminProduct = (id: string) => orNotFound(get<ProductAdmin>(`/api/admin/catalog/products/${encodeURIComponent(id)}`));

export const listCategories = () => get<CategoryOption[]>("/api/admin/catalog/categories");

export async function listBrands(): Promise<BrandOption[]> {
  const page = await get<Paged<BrandOption>>("/api/admin/catalog/brands?size=100");
  return page.items.map(({ id, name }) => ({ id, name }));
}

export const listReviews = (o: { status: string; page: number }) =>
  get<Paged<AdminReviewRow>>(`/api/admin/catalog/reviews${qs({ status: o.status, page: o.page, size: 20 })}`);

export const listInventory = (o: { q?: string; lowStock?: boolean; page: number }) =>
  get<Paged<InventoryRow>>(`/api/admin/inventory${qs({ q: o.q, lowStock: o.lowStock, page: o.page, size: 50 })}`);

export const listFlashSales = (page: number) => get<Paged<AdminFlashSaleRow>>(`/api/admin/catalog/flash-sales${qs({ page, size: 20 })}`);

export const getFlashSale = (id: string) => orNotFound(get<AdminFlashSaleDetail>(`/api/admin/catalog/flash-sales/${encodeURIComponent(id)}`));

export const listGoldRates = (page: number) => get<Paged<GoldRateView>>(`/api/admin/catalog/gold-rates${qs({ page, size: 20 })}`);

/** Giá hiện hành (API công khai, không cache để admin thấy ngay sau khi nhập). */
export const currentGoldRates = () => get<GoldRateView[]>("/api/gold-rates/current");
