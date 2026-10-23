"use server";

import { updateTag } from "next/cache";
import { ApiError } from "@/lib/api";
import {
  parseOptions,
  productSchema,
  variantSchema,
  type ImageAdmin,
  type InventoryRow,
  type ProductAdmin,
  type ProductFormValues,
  type VariantFormValues,
} from "@/lib/admin-shared";
import { CATALOG_CACHE_TAG } from "@/lib/catalog-shared";
import { flashSaleSchema, type FlashSaleFormValues } from "@/lib/flash-sale-shared";
import type { OrderStatus } from "@/lib/checkout-shared";
import { authFetch } from "@/lib/session";

/**
 * Server Action của trang quản trị. Quyền STAFF/ADMIN do backend kiểm tra theo token; lỗi trả về dạng giá trị
 * (message đã dịch + traceId) để hiện cho nhân viên.
 */
export type AdminError = { ok: false; message: string; code?: string; traceId?: string };
type Result<T = object> = ({ ok: true } & T) | AdminError;

const JSON_HEADERS = { "Content-Type": "application/json" };

function toError(error: unknown): AdminError {
  if (error instanceof ApiError) {
    const message = error.code === "common.concurrent-modification" ? "Dữ liệu vừa được người khác sửa, vui lòng tải lại trang." : error.message;
    return { ok: false, message, code: error.code, traceId: error.traceId };
  }
  console.warn("[admin] lỗi không mong đợi:", (error as Error).message);
  return { ok: false, message: "Không kết nối được hệ thống, vui lòng thử lại." };
}

const send = <T>(path: string, method: string, body?: unknown) =>
  authFetch<T>(path, { method, headers: JSON_HEADERS, body: body === undefined ? undefined : JSON.stringify(body) });

/** Sản phẩm đổi thì hết hạn ngay cache catalog của storefront (trang chủ, danh mục, chi tiết). */
function refreshStorefront() {
  updateTag(CATALOG_CACHE_TAG);
}

// ---- Đơn hàng ----

export async function changeOrderStatus(code: string, to: OrderStatus, reason?: string): Promise<Result> {
  try {
    await send(`/api/admin/orders/${encodeURIComponent(code)}/status`, "POST", { to, reason: reason?.trim() || undefined });
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}

export async function shipOrder(code: string, carrier?: string): Promise<Result> {
  try {
    await send(`/api/admin/orders/${encodeURIComponent(code)}/ship`, "POST", { carrier: carrier || undefined });
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}

export async function cancelOrderAdmin(code: string, reason: string): Promise<Result> {
  if (!reason.trim()) return { ok: false, message: "Vui lòng nhập lý do hủy." };
  try {
    await send(`/api/admin/orders/${encodeURIComponent(code)}/cancel`, "POST", { reason: reason.trim().slice(0, 255) });
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}

// ---- Sản phẩm ----

export async function saveProduct(id: string | null, values: ProductFormValues, version?: number): Promise<Result<{ id: string }>> {
  const parsed = productSchema.safeParse(values);
  if (!parsed.success) return { ok: false, message: parsed.error.issues[0]?.message ?? "Thông tin chưa hợp lệ" };
  const v = parsed.data;
  const body = {
    ...v,
    slug: v.slug || undefined,
    brandId: v.brandId || undefined,
    description: v.description?.trim() || undefined,
    seoTitle: v.seoTitle || undefined,
    seoDescription: v.seoDescription || undefined,
    version,
  };
  try {
    const saved = id
      ? await send<ProductAdmin>(`/api/admin/catalog/products/${id}`, "PUT", body)
      : await send<ProductAdmin>("/api/admin/catalog/products", "POST", body);
    refreshStorefront();
    return { ok: true, id: saved.id };
  } catch (error) {
    return toError(error);
  }
}

export async function deleteProduct(id: string): Promise<Result> {
  try {
    await send(`/api/admin/catalog/products/${id}`, "DELETE");
    refreshStorefront();
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}

export async function saveVariant(productId: string, variantId: string | null, values: VariantFormValues, version?: number): Promise<Result> {
  const parsed = variantSchema.safeParse(values);
  if (!parsed.success) return { ok: false, message: parsed.error.issues[0]?.message ?? "Thông tin chưa hợp lệ" };
  const v = parsed.data;
  const body = {
    skuCode: v.skuCode,
    optionValues: parseOptions(v.options),
    price: Number(v.price),
    compareAtPrice: v.compareAtPrice ? Number(v.compareAtPrice) : undefined,
    status: v.status,
    version,
  };
  try {
    const base = `/api/admin/catalog/products/${productId}/variants`;
    await (variantId ? send(`${base}/${variantId}`, "PUT", body) : send(base, "POST", body));
    refreshStorefront();
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}

export async function deleteVariant(productId: string, variantId: string): Promise<Result> {
  try {
    await send(`/api/admin/catalog/products/${productId}/variants/${variantId}`, "DELETE");
    refreshStorefront();
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}

/** Thay toàn bộ danh sách ảnh; thứ tự mảng = thứ tự hiển thị. */
export async function saveImages(productId: string, images: Pick<ImageAdmin, "id" | "variantId" | "url" | "alt">[]): Promise<Result> {
  const items = images
    .map((i) => ({ id: i.id || undefined, variantId: i.variantId || undefined, url: i.url.trim(), alt: i.alt?.trim() || undefined }))
    .filter((i) => i.url);
  if (items.some((i) => i.url.length > 500)) return { ok: false, message: "URL ảnh tối đa 500 ký tự." };
  try {
    await send(`/api/admin/catalog/products/${productId}/images`, "PUT", items);
    refreshStorefront();
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}

// ---- Tồn kho ----

export async function updateStock(skuId: string, onHand: number, lowStockThreshold: number, version: number): Promise<Result<{ row: InventoryRow }>> {
  if (!Number.isInteger(onHand) || onHand < 0 || !Number.isInteger(lowStockThreshold) || lowStockThreshold < 0) {
    return { ok: false, message: "Số lượng phải là số nguyên không âm." };
  }
  try {
    const row = await send<InventoryRow>(`/api/admin/inventory/${skuId}`, "PUT", { onHand, lowStockThreshold, version });
    // Trang chi tiết sản phẩm đọc tồn kho không cache, chỉ nhãn "Hết hàng" trên lưới là cache
    refreshStorefront();
    return { ok: true, row };
  } catch (error) {
    return toError(error);
  }
}

// ---- Đánh giá ----

/** Duyệt / từ chối đánh giá; trang sản phẩm đọc đánh giá qua cache catalog nên làm mới ngay. */
export async function moderateReview(reviewId: string, action: "approve" | "reject", reason?: string): Promise<Result> {
  if (action === "reject" && !reason?.trim()) return { ok: false, message: "Vui lòng nhập lý do." };
  try {
    await send(`/api/admin/catalog/reviews/${encodeURIComponent(reviewId)}/${action}`, "POST", action === "reject" ? { reason: reason!.trim().slice(0, 255) } : undefined);
    refreshStorefront();
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}

// ---- Flash sale (E7.2) ----

/** Tìm SKU để thêm vào chương trình (dùng lại API tồn kho: tìm theo mã SKU hoặc tên sản phẩm). */
export async function searchSkus(q: string): Promise<Result<{ items: InventoryRow[] }>> {
  if (q.trim().length < 2) return { ok: true, items: [] };
  try {
    const page = await authFetch<{ items: InventoryRow[] }>(`/api/admin/inventory?q=${encodeURIComponent(q.trim())}&size=10`, { cache: "no-store" });
    return { ok: true, items: page.items };
  } catch (error) {
    return toError(error);
  }
}

/** Tạo chương trình; {@code values.startsAt/endsAt} đã là ISO do form đổi theo giờ trình duyệt. Giá flash hiện ngay khi tới giờ. */
export async function createFlashSale(values: FlashSaleFormValues): Promise<Result<{ id: string }>> {
  const parsed = flashSaleSchema.safeParse(values);
  if (!parsed.success) return { ok: false, message: parsed.error.issues[0]?.message ?? "Dữ liệu không hợp lệ." };
  const v = parsed.data;
  try {
    const created = await send<{ id: string }>("/api/admin/catalog/flash-sales", "POST", {
      name: v.name,
      startsAt: v.startsAt,
      endsAt: v.endsAt,
      items: v.items.map((i) => ({ skuId: i.skuId, salePrice: Number(i.salePrice), quota: Number(i.quota), perCustomerLimit: Number(i.perCustomerLimit) })),
    });
    refreshStorefront();
    return { ok: true, id: created.id };
  } catch (error) {
    return toError(error);
  }
}

export async function cancelFlashSale(id: string, version: number): Promise<Result> {
  try {
    await send(`/api/admin/catalog/flash-sales/${encodeURIComponent(id)}/cancel`, "POST", { version });
    refreshStorefront();
    return { ok: true };
  } catch (error) {
    return toError(error);
  }
}
