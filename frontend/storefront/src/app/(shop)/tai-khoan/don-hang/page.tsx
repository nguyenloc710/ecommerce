import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { Suspense } from "react";
import { formatDateTime } from "@/components/order/order-items";
import { ORDER_STATUS_LABELS } from "@/lib/checkout-shared";
import { formatVnd } from "@/lib/format";
import { listMyOrders } from "@/lib/orders";
import { requireSession } from "@/lib/session";

export const metadata: Metadata = { title: "Đơn hàng của tôi", robots: { index: false } };

export default function Page({ searchParams }: PageProps<"/tai-khoan/don-hang">) {
  return (
    <main className="pad flex-1 py-12 lg:py-20">
      <Link href="/tai-khoan" className="eyebrow text-muted">
        Tài khoản
      </Link>
      <h1 className="font-display mt-3 text-[36px] font-medium leading-tight lg:text-[48px]">Đơn hàng của tôi</h1>
      <Suspense fallback={<div className="mt-8 h-48 animate-pulse bg-stone" />}>
        <OrderList searchParams={searchParams} />
      </Suspense>
    </main>
  );
}

async function OrderList({ searchParams }: Pick<PageProps<"/tai-khoan/don-hang">, "searchParams">) {
  const { trang } = await searchParams;
  const page = Math.max(0, Number(typeof trang === "string" ? trang : 0) || 0);
  await requireSession("/tai-khoan/don-hang");
  const result = await listMyOrders(page);

  if (result.items.length === 0 && page === 0) {
    return (
      <div className="mt-8 flex flex-col items-start gap-4">
        <p className="m-0 text-[14px] text-muted">Bạn chưa có đơn hàng nào.</p>
        <Link href="/moi-ve" className="btn">
          Xem hàng mới về
        </Link>
      </div>
    );
  }
  return (
    <>
      <ul className="mt-8 flex list-none flex-col border-t border-line p-0">
        {result.items.map((o) => (
          <li key={o.id}>
            <Link href={`/tai-khoan/don-hang/${encodeURIComponent(o.code)}`} className="flex items-center gap-4 border-b border-line py-5 text-[13px] hover:bg-bone">
              <span className="relative h-[80px] w-[60px] flex-none bg-stone">
                {o.firstItemImage ? <Image src={o.firstItemImage} alt={o.firstItemName} fill sizes="60px" className="object-cover" /> : null}
              </span>
              <span className="flex flex-1 flex-col gap-0.5">
                <span>
                  {o.code} <span className="text-muted">· {formatDateTime(o.placedAt)}</span>
                </span>
                <span className="text-muted">
                  {o.firstItemName}
                  {/* itemCount là tổng số lượng, không phải số dòng hàng */}
                  {o.itemCount > 1 ? ` · ${o.itemCount} sản phẩm` : ""}
                </span>
              </span>
              <span className="flex flex-col items-end gap-0.5">
                <span>{formatVnd(o.total)}</span>
                <span className={`eyebrow text-[10px] ${o.status === "CANCELLED" ? "text-sale" : "text-muted"}`}>{ORDER_STATUS_LABELS[o.status] ?? o.status}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
      <nav aria-label="Phân trang" className="mt-6 flex gap-6">
        {page > 0 ? (
          <Link href={`/tai-khoan/don-hang?trang=${page - 1}`} className="eyebrow ul">
            Trang trước
          </Link>
        ) : null}
        {result.hasNext ? (
          <Link href={`/tai-khoan/don-hang?trang=${page + 1}`} className="eyebrow ul">
            Trang sau
          </Link>
        ) : null}
      </nav>
    </>
  );
}
