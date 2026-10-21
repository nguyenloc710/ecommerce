import "server-only";
import { notFound } from "next/navigation";
import { ApiError } from "@/lib/api";
import type { OrderSummary, OrderView } from "@/lib/checkout-shared";
import { authFetch } from "@/lib/session";

/** Đơn của khách đang đăng nhập theo mã; đơn không tồn tại hoặc của người khác → trang 404. */
export async function getMyOrder(code: string): Promise<OrderView> {
  try {
    return await authFetch<OrderView>(`/api/orders/${encodeURIComponent(code)}`, { cache: "no-store" });
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  }
}

export type OrderPage = { items: OrderSummary[]; page: number; size: number; total: number; hasNext: boolean };

export async function listMyOrders(page: number, size = 10): Promise<OrderPage> {
  return authFetch<OrderPage>(`/api/orders/me?page=${page}&size=${size}`, { cache: "no-store" });
}
