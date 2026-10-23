import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { AdminHeader, Badge } from "@/components/admin/admin-ui";
import { FlashSaleCancel } from "@/components/admin/flash-sale-cancel";
import { formatDateTime } from "@/components/order/order-items";
import { getFlashSale } from "@/lib/admin-data";
import { PHASE_TEXT, PHASE_TONE } from "@/lib/flash-sale-shared";
import { formatVnd } from "@/lib/format";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Chi tiết flash sale" };

export default function Page({ params }: PageProps<"/admin/flash-sale/[id]">) {
  return (
    <Suspense fallback={<div className="card h-64 animate-pulse" />}>
      <Detail params={params} />
    </Suspense>
  );
}

/** Theo dõi suất theo SKU: đã bán (đơn đã xác nhận) và đang giữ (đơn chờ thanh toán, tự trả khi hết hạn). */
async function Detail({ params }: Pick<PageProps<"/admin/flash-sale/[id]">, "params">) {
  const { id } = await params;
  await requireStaff(`/admin/flash-sale/${id}`);
  const sale = await getFlashSale(id);
  const cancellable = sale.phase === "UPCOMING" || sale.phase === "RUNNING";

  return (
    <>
      <AdminHeader
        title={sale.name}
        back={{ href: "/admin/flash-sale", label: "Flash sale" }}
        actions={cancellable ? <FlashSaleCancel id={sale.id} version={sale.version} /> : null}
      />
      <div className="card mb-4 flex flex-wrap items-center gap-x-6 gap-y-2 p-4 text-[13px]">
        <Badge tone={PHASE_TONE[sale.phase]}>{PHASE_TEXT[sale.phase]}</Badge>
        <span>
          <span className="text-muted">Bắt đầu </span>
          {formatDateTime(sale.startsAt)}
        </span>
        <span>
          <span className="text-muted">Kết thúc </span>
          {formatDateTime(sale.endsAt)}
        </span>
      </div>
      <div className="card overflow-x-auto">
        <table className="w-full min-w-[760px] border-collapse text-[13px]">
          <thead>
            <tr className="bg-admin-bg text-left text-[12px] text-muted">
              <th className="px-4 py-2 font-medium">Sản phẩm</th>
              <th className="px-4 py-2 font-medium">Giá niêm yết</th>
              <th className="px-4 py-2 font-medium">Giá flash</th>
              <th className="px-4 py-2 font-medium">Đã bán</th>
              <th className="px-4 py-2 font-medium">Đang giữ</th>
              <th className="px-4 py-2 font-medium">Số suất</th>
              <th className="px-4 py-2 font-medium">Mỗi khách</th>
            </tr>
          </thead>
          <tbody>
            {sale.items.map((i) => (
              <tr key={i.itemId} className="border-t border-admin-line">
                <td className="px-4 py-2.5">
                  {i.productSlug ? (
                    <Link href={`/san-pham/${i.productSlug}`} target="_blank" className="hover:underline">
                      {i.productName}
                    </Link>
                  ) : (
                    (i.productName ?? i.skuId)
                  )}
                  <span className="text-muted">
                    {i.variantLabel ? ` · ${i.variantLabel}` : ""} {i.skuCode ? `(${i.skuCode})` : ""}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-muted">{i.listPrice !== null ? formatVnd(i.listPrice) : "—"}</td>
                <td className="px-4 py-2.5">{formatVnd(i.salePrice)}</td>
                <td className="px-4 py-2.5">{i.sold}</td>
                <td className="px-4 py-2.5">{i.held}</td>
                <td className="px-4 py-2.5">{i.quota}</td>
                <td className="px-4 py-2.5">{i.perCustomerLimit === 0 ? "Không giới hạn" : i.perCustomerLimit}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
