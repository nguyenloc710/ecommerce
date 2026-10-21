import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { CancelOrderButton } from "@/components/order/cancel-order-button";
import { OrderItems, formatDateTime } from "@/components/order/order-items";
import { CUSTOMER_CANCELLABLE, ORDER_STATUS_LABELS, PAYMENT_LABELS, cancelReasonLabel } from "@/lib/checkout-shared";
import { getMyOrder } from "@/lib/orders";
import { requireSession } from "@/lib/session";

export const metadata: Metadata = { title: "Chi tiết đơn hàng", robots: { index: false } };

export default function Page({ params }: PageProps<"/tai-khoan/don-hang/[code]">) {
  return (
    <main className="pad mx-auto flex w-full max-w-[860px] flex-1 flex-col gap-8 py-12 lg:py-20">
      <Link href="/tai-khoan/don-hang" className="eyebrow text-muted">
        ← Đơn hàng của tôi
      </Link>
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone" />}>
        <OrderDetail params={params} />
      </Suspense>
    </main>
  );
}

async function OrderDetail({ params }: Pick<PageProps<"/tai-khoan/don-hang/[code]">, "params">) {
  const { code } = await params;
  await requireSession(`/tai-khoan/don-hang/${code}`);
  const order = await getMyOrder(code);
  const a = order.shippingAddress;
  const payUrl = `/thanh-toan/mock/${order.id}?ma=${encodeURIComponent(order.code)}`;

  return (
    <>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="font-display m-0 text-[32px] font-medium leading-tight lg:text-[44px]">{order.code}</h1>
          <span className="text-[13px] text-muted">Đặt lúc {formatDateTime(order.placedAt)}</span>
        </div>
        <span className={`eyebrow ${order.status === "CANCELLED" ? "text-sale" : ""}`}>{ORDER_STATUS_LABELS[order.status] ?? order.status}</span>
      </div>

      {order.status === "AWAITING_PAYMENT" && order.paymentMethod === "MOCK_ONLINE" ? (
        <div className="flex flex-wrap items-center justify-between gap-4 bg-bone px-5 py-4 text-[13px]">
          <span>Đơn đang chờ thanh toán, sẽ tự hủy sau 15 phút kể từ lúc đặt.</span>
          <Link href={payUrl} className="btn min-h-11">
            Thanh toán ngay
          </Link>
        </div>
      ) : null}
      {order.status === "CANCELLED" && order.cancelReason ? <p className="m-0 text-[13px] text-sale">Lý do hủy: {cancelReasonLabel(order.cancelReason)}</p> : null}

      <div className="grid gap-6 border-y border-line py-6 text-[13px] sm:grid-cols-3">
        <div className="flex flex-col gap-1">
          <span className="eyebrow">Giao tới</span>
          <span>
            {a.recipientName} · {a.phone}
          </span>
          <span className="text-muted">
            {a.line1}, {a.wardName}, {a.provinceName}
          </span>
        </div>
        <div className="flex flex-col gap-1">
          <span className="eyebrow">Thanh toán</span>
          <span>{PAYMENT_LABELS[order.paymentMethod]?.title ?? order.paymentMethod}</span>
          {order.paidAt ? <span className="text-muted">Đã thanh toán {formatDateTime(order.paidAt)}</span> : null}
        </div>
        <div className="flex flex-col gap-1">
          <span className="eyebrow">Vận chuyển</span>
          <span>{order.shipment ? `${order.shipment.carrier} · ${order.shipment.trackingCode}` : order.shippingCarrier}</span>
          {!order.shipment ? <span className="text-muted">Mã vận đơn có khi shop giao hàng</span> : null}
        </div>
      </div>

      <OrderItems order={order} />

      {order.history.length > 0 ? (
        <section className="flex flex-col gap-3">
          <h2 className="eyebrow m-0">Lịch sử</h2>
          <ol className="m-0 flex list-none flex-col gap-2 p-0 text-[13px]">
            {[...order.history].reverse().map((h) => (
              <li key={`${h.to}-${h.at}`} className="flex gap-4">
                <span className="w-[130px] flex-none text-muted">{formatDateTime(h.at)}</span>
                <span>{ORDER_STATUS_LABELS[h.to] ?? h.to}</span>
              </li>
            ))}
          </ol>
        </section>
      ) : null}

      {CUSTOMER_CANCELLABLE.includes(order.status) ? <CancelOrderButton orderCode={order.code} /> : null}
    </>
  );
}
