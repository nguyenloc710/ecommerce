"use server";

import { ApiError } from "@/lib/api";
import { getAvailability, getProduct } from "@/lib/catalog";
import { authFetch, getAccessToken } from "@/lib/session";

/**
 * Server Action cho giỏ hàng. Khách đăng nhập: giỏ ở order-service (giá/tồn tra lại mỗi lần, B-16).
 * Khách vãng lai: giỏ ở trình duyệt, chỉ hỏi catalog để cập nhật giá và tồn kho.
 * Lỗi trả về dạng giá trị (không throw) vì message lỗi của Server Action bị ẩn ở production.
 */

export type CartLineInput = { skuId: string; productSlug: string; quantity: number; engraving?: string };

export type SyncedLine = {
  skuId: string;
  productSlug: string;
  name: string;
  variantLabel: string;
  price: number;
  imageUrl?: string;
  qty: number;
  engraving?: string;
  /** Số còn mua được; null = không giới hạn hiển thị (còn nhiều). */
  available: number | null;
  sellable: boolean;
};

export type CartIssue = { skuId: string; message: string };
export type SyncedCart = { lines: SyncedLine[]; issues: CartIssue[] };
export type CartResult = { ok: true; cart: SyncedCart } | { ok: false; message: string; traceId?: string };

/** Theo CartView của order-service. */
type CartView = {
  lines: {
    skuId: string;
    productSlug: string;
    productName: string;
    variantLabel: string;
    imageUrl: string | null;
    unitPrice: number;
    quantity: number;
    engraving: string | null;
    available: number;
    sellable: boolean;
  }[];
  issues: { skuId: string; code: string; message: string }[];
};

const MAX_LINES = 100; // khớp @Size của CartMergeCommand

function toSynced(view: CartView): SyncedCart {
  return {
    lines: view.lines.map((l) => ({
      skuId: l.skuId,
      productSlug: l.productSlug,
      name: l.productName,
      variantLabel: l.variantLabel,
      price: Number(l.unitPrice),
      imageUrl: l.imageUrl ?? undefined,
      qty: l.quantity,
      engraving: l.engraving ?? undefined,
      available: l.available,
      sellable: l.sellable,
    })),
    issues: view.issues.map((i) => ({ skuId: i.skuId, message: i.message })),
  };
}

function failure(error: unknown): CartResult {
  if (error instanceof ApiError) return { ok: false, message: error.message, traceId: error.traceId };
  console.warn("[cart] lỗi gọi order-service:", (error as Error).message);
  return { ok: false, message: "Không cập nhật được giỏ hàng, vui lòng thử lại." };
}

function sanitize(lines: CartLineInput[]): CartLineInput[] {
  return lines.slice(0, MAX_LINES).map((l) => ({
    skuId: String(l.skuId),
    productSlug: String(l.productSlug),
    quantity: Math.max(0, Math.min(999, Math.trunc(Number(l.quantity) || 0))),
    engraving: l.engraving?.trim() || undefined,
  }));
}

/**
 * Gọi khi trang tải xong. Chưa đăng nhập → null (giữ giỏ trình duyệt). Đã đăng nhập → gộp giỏ vãng lai (nếu có)
 * vào giỏ server rồi trả giỏ server; FE xóa giỏ trình duyệt sau khi gộp thành công.
 */
export async function syncCart(guestLines: CartLineInput[]): Promise<CartResult | null> {
  if (!(await getAccessToken())) return null;
  try {
    const lines = sanitize(guestLines).filter((l) => l.quantity > 0);
    const view =
      lines.length > 0
        ? await authFetch<CartView>("/api/cart/merge", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ lines }) })
        : await authFetch<CartView>("/api/cart", { cache: "no-store" });
    return { ok: true, cart: toSynced(view) };
  } catch (error) {
    return failure(error);
  }
}

/** Đặt số lượng một dòng giỏ server; quantity = 0 xóa dòng. */
export async function setServerLine(line: CartLineInput): Promise<CartResult> {
  try {
    const [body] = sanitize([line]);
    const view = await authFetch<CartView>("/api/cart/items", { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
    return { ok: true, cart: toSynced(view) };
  } catch (error) {
    return failure(error);
  }
}

/** Giỏ vãng lai: lấy giá hiện tại (cache catalog 1 phút) và tồn kho thật (không cache) cho từng dòng. */
export async function refreshGuestCart(lines: CartLineInput[]): Promise<SyncedCart | null> {
  const input = sanitize(lines);
  if (input.length === 0) return null;
  const slugs = [...new Set(input.map((l) => l.productSlug))];
  const [products, availability] = await Promise.all([Promise.all(slugs.map((s) => getProduct(s))), getAvailability(input.map((l) => l.skuId))]);
  const bySlug = new Map(slugs.map((s, i) => [s, products[i]]));

  const issues: CartIssue[] = [];
  const synced: SyncedLine[] = [];
  for (const l of input) {
    const product = bySlug.get(l.productSlug);
    const variant = product?.variants.find((v) => v.id === l.skuId);
    if (!product || !variant) {
      issues.push({ skuId: l.skuId, message: "Sản phẩm không còn bán, vui lòng xóa khỏi giỏ." });
      continue;
    }
    const stock = availability[l.skuId];
    const sellable = stock?.inStock ?? true; // không lấy được tồn kho → không chặn, checkout kiểm tra lại
    const available = stock?.remaining ?? null;
    if (!sellable) issues.push({ skuId: l.skuId, message: `${product.name}: đã hết hàng.` });
    else if (available !== null && l.quantity > available) issues.push({ skuId: l.skuId, message: `${product.name}: chỉ còn ${available} sản phẩm.` });
    synced.push({
      skuId: l.skuId,
      productSlug: product.slug,
      name: product.name,
      variantLabel: Object.values(variant.optionValues).join(" / "),
      price: variant.price,
      imageUrl: product.images.find((i) => i.variantId === variant.id)?.url ?? product.images[0]?.url,
      qty: l.quantity,
      engraving: l.engraving,
      available,
      sellable,
    });
  }
  return { lines: synced, issues };
}
