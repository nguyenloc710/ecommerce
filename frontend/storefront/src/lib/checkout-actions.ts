"use server";

import { createHmac, randomUUID } from "node:crypto";
import { ApiError, apiFetch } from "@/lib/api";
import {
  CUSTOMER_CANCELLABLE,
  checkoutSchema,
  type CheckoutInput,
  type CheckoutLine,
  type CheckoutQuote,
  type Division,
  type OrderView,
} from "@/lib/checkout-shared";
import { getProvinces, getWards } from "@/lib/divisions";
import { authFetch } from "@/lib/session";

/**
 * Server Action cho thanh toán (E5.6). Lỗi trả về dạng giá trị: message đã dịch từ backend + traceId để khách báo lỗi.
 */
export type ActionError = { ok: false; message: string; code?: string; traceId?: string };
type Result<T> = ({ ok: true } & T) | ActionError;

const JSON_HEADERS = { "Content-Type": "application/json" };
const MAX_LINES = 50; // khớp @Size của CheckoutRequest.lines

function toError(error: unknown): ActionError {
  if (error instanceof ApiError) return { ok: false, message: error.message, code: error.code, traceId: error.traceId };
  console.warn("[checkout] lỗi không mong đợi:", (error as Error).message);
  return { ok: false, message: "Không kết nối được hệ thống, vui lòng thử lại." };
}

function cleanLines(lines: CheckoutLine[]): CheckoutLine[] {
  return lines.slice(0, MAX_LINES).map((l) => ({
    skuId: String(l.skuId),
    quantity: Math.max(1, Math.min(999, Math.trunc(Number(l.quantity) || 1))),
    engraving: l.engraving?.trim() || undefined,
  }));
}

export async function loadWards(provinceCode: string): Promise<Division[]> {
  try {
    return await getWards(provinceCode);
  } catch {
    return [];
  }
}

/** Báo giá: không giữ hàng, không tạo gì. Voucher lỗi không làm hỏng quote (trả voucherError). */
export async function quoteCheckout(
  lines: CheckoutLine[],
  opts: { addressId?: string; shippingCarrier?: string; voucherCode?: string },
): Promise<Result<{ quote: CheckoutQuote }>> {
  try {
    const quote = await authFetch<CheckoutQuote>("/api/checkout/quote", {
      method: "POST",
      headers: JSON_HEADERS,
      body: JSON.stringify({ lines: cleanLines(lines), ...opts }),
      cache: "no-store",
    });
    return { ok: true, quote };
  } catch (error) {
    return toError(error);
  }
}

/** Áp mã: gọi validate để có message lỗi đã dịch (quote chỉ trả message key). */
export async function validateVoucher(code: string, subtotal: number): Promise<Result<{ discount: number; description: string | null }>> {
  try {
    const res = await authFetch<{ discount: number; description: string | null }>("/api/vouchers/validate", {
      method: "POST",
      headers: JSON_HEADERS,
      body: JSON.stringify({ code: code.trim().toUpperCase(), subtotal }),
    });
    return { ok: true, discount: res.discount, description: res.description };
  } catch (error) {
    return toError(error);
  }
}

/**
 * Đặt hàng. `idempotencyKey` do trình duyệt sinh mỗi lần bấm "Đặt hàng" (B-17): bấm lại sau lỗi mạng
 * với cùng key sẽ nhận lại đơn cũ thay vì tạo đơn mới.
 */
export async function placeOrder(lines: CheckoutLine[], input: CheckoutInput, idempotencyKey: string): Promise<Result<{ code: string; next: string }>> {
  const parsed = checkoutSchema.safeParse(input);
  if (!parsed.success) return { ok: false, message: parsed.error.issues[0]?.message ?? "Thông tin chưa hợp lệ" };
  const v = parsed.data;
  try {
    let addressId = v.addressId;
    let address: Record<string, string | undefined> | undefined;
    if (!addressId && v.address) {
      if (v.saveAddress) {
        // Lưu vào sổ địa chỉ rồi đặt bằng addressId; customer-service tự tra tên tỉnh/phường
        const saved = await authFetch<{ id: string }>("/api/customers/me/addresses", { method: "POST", headers: JSON_HEADERS, body: JSON.stringify(v.address) });
        addressId = saved.id;
      } else {
        const [provinces, wards] = await Promise.all([getProvinces(), getWards(v.address.provinceCode)]);
        const province = provinces.find((p) => p.code === v.address!.provinceCode);
        const ward = wards.find((w) => w.code === v.address!.wardCode);
        if (!province || !ward) return { ok: false, message: "Tỉnh/phường không hợp lệ, vui lòng chọn lại." };
        address = { ...v.address, provinceName: province.name, wardName: ward.name };
      }
    }
    const res = await authFetch<{ order: OrderView; payUrl: string | null; replayed: boolean }>("/api/orders", {
      method: "POST",
      headers: { ...JSON_HEADERS, "Idempotency-Key": idempotencyKey },
      body: JSON.stringify({
        lines: cleanLines(lines),
        addressId,
        address,
        paymentMethod: v.paymentMethod,
        shippingCarrier: v.shippingCarrier,
        voucherCode: v.voucherCode || undefined,
        contactEmail: v.contactEmail,
        note: v.note || undefined,
      }),
      timeoutMs: 15_000,
    });
    const { order } = res;
    // payUrl chỉ có ở lần tạo đầu; đơn online chưa trả (kể cả lần gọi lại) thì vẫn đưa sang trang thanh toán
    const next =
      order.status === "AWAITING_PAYMENT" ? `/thanh-toan/mock/${order.id}?ma=${encodeURIComponent(order.code)}` : `/thanh-toan/thanh-cong/${encodeURIComponent(order.code)}`;
    return { ok: true, code: order.code, next };
  } catch (error) {
    return toError(error);
  }
}

/**
 * Cổng thanh toán giả (B-18): ký HMAC-SHA256 body bằng secret dùng chung với order-service rồi gọi webhook,
 * giống cách VNPay/MoMo báo kết quả. Chỉ bật khi có PAYMENT_MOCK_SECRET (không cấu hình ở production).
 */
export async function completeMockPayment(orderCode: string, success: boolean): Promise<Result<{ next: string }>> {
  const secret = process.env.PAYMENT_MOCK_SECRET;
  if (!secret) return { ok: false, message: "Cổng thanh toán thử nghiệm chưa được bật." };
  try {
    const order = await authFetch<OrderView>(`/api/orders/${encodeURIComponent(orderCode)}`, { cache: "no-store" });
    if (order.status !== "AWAITING_PAYMENT") return { ok: true, next: `/tai-khoan/don-hang/${encodeURIComponent(order.code)}` };
    const body = JSON.stringify({ orderId: order.id, txnId: `MOCK-${randomUUID()}`, status: success ? "SUCCESS" : "FAILED", amount: order.total, reason: success ? undefined : "declined" });
    const signature = createHmac("sha256", secret).update(body, "utf8").digest("hex");
    await apiFetch<void>("/webhooks/payments/mock", { method: "POST", headers: { ...JSON_HEADERS, "X-Signature": signature }, body });
    return { ok: true, next: success ? `/thanh-toan/thanh-cong/${encodeURIComponent(order.code)}` : `/tai-khoan/don-hang/${encodeURIComponent(order.code)}` };
  } catch (error) {
    return toError(error);
  }
}

export async function cancelMyOrder(orderCode: string, reason: string): Promise<Result<{ status: string }>> {
  try {
    const order = await authFetch<OrderView>(`/api/orders/${encodeURIComponent(orderCode)}`, { cache: "no-store" });
    if (!CUSTOMER_CANCELLABLE.includes(order.status)) return { ok: false, message: "Đơn hàng đã được xử lý, không thể hủy." };
    const res = await authFetch<OrderView>(`/api/orders/${encodeURIComponent(orderCode)}/cancel`, {
      method: "POST",
      headers: JSON_HEADERS,
      body: JSON.stringify({ reason: reason.trim().slice(0, 255) || undefined }),
    });
    return { ok: true, status: res.status };
  } catch (error) {
    return toError(error);
  }
}
