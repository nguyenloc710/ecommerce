import Image from "next/image";
import Link from "next/link";
import type { OrderView } from "@/lib/checkout-shared";
import { formatVnd } from "@/lib/format";

/** Danh sách hàng + tổng tiền của một đơn (dùng ở trang thành công và chi tiết đơn). */
export function OrderItems({ order }: { order: OrderView }) {
  return (
    <div className="flex flex-col gap-5">
      <ul className="m-0 flex list-none flex-col p-0">
        {order.items.map((i) => (
          <li key={`${i.skuId}:${i.engraving ?? ""}`} className="flex gap-4 border-b border-line py-4 text-[13px]">
            <Link href={`/san-pham/${i.productSlug}`} className="relative h-[96px] w-[72px] flex-none bg-stone">
              {i.imageUrl ? <Image src={i.imageUrl} alt={i.productName} fill sizes="72px" className="object-cover" /> : null}
            </Link>
            <span className="flex flex-1 flex-col gap-0.5">
              <Link href={`/san-pham/${i.productSlug}`}>{i.productName}</Link>
              <span className="text-[12px] text-muted">
                {i.variantLabel}
                {i.engraving ? ` · Khắc "${i.engraving}"` : ""} · SL {i.quantity}
              </span>
            </span>
            <span>{formatVnd(i.lineTotal)}</span>
          </li>
        ))}
      </ul>
      <dl className="m-0 ml-auto flex w-full max-w-[360px] flex-col gap-2 text-[13px]">
        <Row label="Tạm tính" value={formatVnd(order.subtotal)} />
        {order.discount > 0 ? <Row label={`Giảm giá${order.voucherCode ? ` (${order.voucherCode})` : ""}`} value={`−${formatVnd(order.discount)}`} /> : null}
        <Row label="Vận chuyển" value={order.shippingFee === 0 ? "Miễn phí" : formatVnd(order.shippingFee)} />
        <div className="mt-1 flex items-baseline justify-between border-t border-line pt-3">
          <dt>Tổng cộng</dt>
          <dd className="font-display m-0 text-[24px] leading-none">{formatVnd(order.total)}</dd>
        </div>
      </dl>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <dt className="text-muted">{label}</dt>
      <dd className="m-0">{value}</dd>
    </div>
  );
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short", timeZone: "Asia/Ho_Chi_Minh" }).format(new Date(iso));
}
