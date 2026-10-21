import type { Metadata } from "next";
import Image from "next/image";
import { Suspense } from "react";
import { AdminHeader, Badge, orderTone } from "@/components/admin/admin-ui";
import { OrderActions } from "@/components/admin/order-actions";
import { formatDateTime } from "@/components/order/order-items";
import { getAdminOrder } from "@/lib/admin-data";
import { ORDER_STATUS_LABELS, PAYMENT_LABELS, cancelReasonLabel } from "@/lib/checkout-shared";
import { formatVnd } from "@/lib/format";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Chi tiết đơn" };

export default function Page({ params }: PageProps<"/admin/don-hang/[code]">) {
  return (
    <Suspense fallback={<div className="card h-64 animate-pulse" />}>
      <OrderDetail params={params} />
    </Suspense>
  );
}

async function OrderDetail({ params }: Pick<PageProps<"/admin/don-hang/[code]">, "params">) {
  const { code } = await params;
  await requireStaff(`/admin/don-hang/${code}`);
  const order = await getAdminOrder(code);
  const a = order.shippingAddress;

  return (
    <>
      <AdminHeader
        title={order.code}
        back={{ href: "/admin/don-hang", label: "Đơn hàng" }}
        actions={<Badge tone={orderTone(order.status)}>{ORDER_STATUS_LABELS[order.status] ?? order.status}</Badge>}
      />
      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <div className="flex flex-col gap-4">
          <section className="card flex flex-col gap-3 p-4">
            <h2 className="m-0 text-[14px] font-semibold">Thao tác</h2>
            <OrderActions code={order.code} status={order.status} defaultCarrier={order.shippingCarrier} />
            {order.cancelReason ? <p className="m-0 text-[13px] text-sale">Lý do hủy: {cancelReasonLabel(order.cancelReason)}</p> : null}
          </section>

          <section className="card p-4">
            <h2 className="m-0 mb-2 text-[14px] font-semibold">Sản phẩm</h2>
            <ul className="m-0 flex list-none flex-col p-0">
              {order.items.map((i) => (
                <li key={`${i.skuId}:${i.engraving ?? ""}`} className="flex items-center gap-3 border-t border-admin-line py-3 text-[13px] first:border-t-0">
                  <span className="relative h-12 w-12 flex-none overflow-hidden rounded-lg bg-admin-bg">
                    {i.imageUrl ? <Image src={i.imageUrl} alt={i.productName} fill sizes="48px" className="object-cover" /> : null}
                  </span>
                  <span className="flex flex-1 flex-col">
                    <span>{i.productName}</span>
                    <span className="text-[12px] text-muted">
                      {i.variantLabel} · SKU {i.skuCode}
                      {i.engraving ? ` · Khắc "${i.engraving}"` : ""}
                    </span>
                  </span>
                  <span className="text-muted">
                    {formatVnd(i.unitPrice)} × {i.quantity}
                  </span>
                  <span className="w-[100px] text-right">{formatVnd(i.lineTotal)}</span>
                </li>
              ))}
            </ul>
            <dl className="m-0 ml-auto mt-3 flex max-w-[300px] flex-col gap-1 border-t border-admin-line pt-3 text-[13px]">
              <Row label="Tạm tính" value={formatVnd(order.subtotal)} />
              {order.discount > 0 ? <Row label={`Giảm giá (${order.voucherCode})`} value={`−${formatVnd(order.discount)}`} /> : null}
              <Row label="Vận chuyển" value={formatVnd(order.shippingFee)} />
              <Row label="Tổng cộng" value={formatVnd(order.total)} strong />
            </dl>
          </section>

          <section className="card p-4">
            <h2 className="m-0 mb-2 text-[14px] font-semibold">Lịch sử</h2>
            <ol className="m-0 flex list-none flex-col gap-2 p-0 text-[13px]">
              {[...order.history].reverse().map((h) => (
                <li key={`${h.to}-${h.at}`} className="flex gap-3">
                  <span className="w-[120px] flex-none text-muted">{formatDateTime(h.at)}</span>
                  <span>
                    {ORDER_STATUS_LABELS[h.to] ?? h.to}
                    {h.reason ? <span className="text-muted"> · {cancelReasonLabel(h.reason)}</span> : null}
                  </span>
                </li>
              ))}
            </ol>
          </section>
        </div>

        <div className="flex flex-col gap-4">
          <section className="card flex flex-col gap-1 p-4 text-[13px]">
            <h2 className="m-0 mb-1 text-[14px] font-semibold">Khách hàng</h2>
            <span>{order.contactEmail}</span>
            {order.note ? <p className="m-0 mt-2 rounded-lg bg-admin-bg p-2">Ghi chú: {order.note}</p> : null}
          </section>
          <section className="card flex flex-col gap-1 p-4 text-[13px]">
            <h2 className="m-0 mb-1 text-[14px] font-semibold">Giao hàng</h2>
            <span>
              {a.recipientName} · {a.phone}
            </span>
            <span className="text-muted">
              {a.line1}, {a.wardName}, {a.provinceName}
            </span>
            <span className="mt-2">
              {order.shipment ? (
                <>
                  {order.shipment.carrier} · <strong>{order.shipment.trackingCode}</strong> ({order.shipment.status})
                </>
              ) : (
                <>Hãng khách chọn: {order.shippingCarrier}</>
              )}
            </span>
          </section>
          <section className="card flex flex-col gap-1 p-4 text-[13px]">
            <h2 className="m-0 mb-1 text-[14px] font-semibold">Thanh toán</h2>
            <span>{PAYMENT_LABELS[order.paymentMethod]?.title ?? order.paymentMethod}</span>
            <span className="text-muted">{order.paidAt ? `Đã thanh toán ${formatDateTime(order.paidAt)}` : "Chưa thanh toán"}</span>
          </section>
        </div>
      </div>
    </>
  );
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className={`flex justify-between ${strong ? "font-semibold" : ""}`}>
      <dt className={strong ? "" : "text-muted"}>{label}</dt>
      <dd className="m-0">{value}</dd>
    </div>
  );
}
