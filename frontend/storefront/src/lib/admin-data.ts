import "server-only";
import { notFound } from "next/navigation";
import { ApiError } from "@/lib/api";
import type { AdminOrderRow, BrandOption, CategoryOption, InventoryRow, Paged, ProductAdmin, ProductAdminRow } from "@/lib/admin-shared";
import type { OrderStatus, OrderView } from "@/lib/checkout-shared";
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

export const listInventory = (o: { q?: string; lowStock?: boolean; page: number }) =>
  get<Paged<InventoryRow>>(`/api/admin/inventory${qs({ q: o.q, lowStock: o.lowStock, page: o.page, size: 50 })}`);
