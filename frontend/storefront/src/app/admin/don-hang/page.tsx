import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { formatDateTime } from "@/components/order/order-items";
import { AdminHeader, Badge, Pager, orderTone, pageParam, textParam } from "@/components/admin/admin-ui";
import { listAdminOrders } from "@/lib/admin-data";
import { ORDER_TABS } from "@/lib/admin-shared";
import { ORDER_STATUS_LABELS } from "@/lib/checkout-shared";
import { formatVnd } from "@/lib/format";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Đơn hàng" };

export default function Page({ searchParams }: PageProps<"/admin/don-hang">) {
  return (
    <>
      <AdminHeader title="Đơn hàng" />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <Orders searchParams={searchParams} />
      </Suspense>
    </>
  );
}

async function Orders({ searchParams }: Pick<PageProps<"/admin/don-hang">, "searchParams">) {
  const sp = await searchParams;
  const tab = ORDER_TABS.find((t) => t.key === textParam(sp.tab)) ?? ORDER_TABS[0];
  const q = textParam(sp.q);
  const page = pageParam(sp.trang);
  await requireStaff("/admin/don-hang");
  const result = await listAdminOrders({ status: tab.status, q: q || undefined, page });

  return (
    <div className="card overflow-hidden">
      <div className="flex flex-col gap-3 border-b border-admin-line p-3 lg:flex-row lg:items-center lg:justify-between">
        <nav aria-label="Lọc theo trạng thái" className="flex gap-1 overflow-x-auto">
          {ORDER_TABS.map((t) => (
            <Link
              key={t.key}
              href={`/admin/don-hang?tab=${t.key}${q ? `&q=${encodeURIComponent(q)}` : ""}`}
              aria-current={t.key === tab.key ? "page" : undefined}
              className={`whitespace-nowrap rounded-lg px-3 py-1.5 text-[13px] ${t.key === tab.key ? "bg-admin-line font-medium" : "text-muted hover:bg-admin-bg"}`}
            >
              {t.label}
            </Link>
          ))}
        </nav>
        <form action="/admin/don-hang" className="flex gap-2">
          <input type="hidden" name="tab" value={tab.key} />
          <input name="q" defaultValue={q} placeholder="Mã đơn, email, SĐT, tên" aria-label="Tìm đơn" className="input-admin lg:w-[260px]" />
          <button type="submit" className="btn-admin btn-admin-light">
            Tìm
          </button>
        </form>
      </div>

      {result.items.length === 0 ? (
        <p className="m-0 px-4 py-10 text-center text-[13px] text-muted">Không có đơn nào.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] border-collapse text-[13px]">
            <thead>
              <tr className="bg-admin-bg text-left text-[12px] text-muted">
                <th className="px-4 py-2 font-medium">Đơn hàng</th>
                <th className="px-4 py-2 font-medium">Ngày đặt</th>
                <th className="px-4 py-2 font-medium">Khách hàng</th>
                <th className="px-4 py-2 font-medium">Thanh toán</th>
                <th className="px-4 py-2 font-medium">Trạng thái</th>
                <th className="px-4 py-2 text-right font-medium">Tổng</th>
              </tr>
            </thead>
            <tbody>
              {result.items.map((o) => (
                <tr key={o.id} className="border-t border-admin-line hover:bg-admin-bg">
                  <td className="px-4 py-3">
                    <Link href={`/admin/don-hang/${encodeURIComponent(o.code)}`} className="font-medium hover:underline">
                      {o.code}
                    </Link>
                    <div className="text-[12px] text-muted">
                      {o.firstItemName}
                      {o.itemCount > 1 ? ` · ${o.itemCount} sản phẩm` : ""}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDateTime(o.placedAt)}</td>
                  <td className="px-4 py-3">
                    {o.recipientName}
                    <div className="text-[12px] text-muted">{o.contactEmail}</div>
                  </td>
                  <td className="px-4 py-3">{o.paymentMethod === "COD" ? "COD" : "Online"}</td>
                  <td className="px-4 py-3">
                    <Badge tone={orderTone(o.status)}>{ORDER_STATUS_LABELS[o.status] ?? o.status}</Badge>
                  </td>
                  <td className="px-4 py-3 text-right">{formatVnd(o.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pager basePath="/admin/don-hang" params={{ tab: tab.key, q }} page={page} hasNext={result.hasNext} total={result.total} />
    </div>
  );
}
