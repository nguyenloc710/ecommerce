import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { AdminHeader, Badge, Pager, pageParam, textParam } from "@/components/admin/admin-ui";
import { RefundComplete } from "@/components/admin/refund-actions";
import { formatDateTime } from "@/components/order/order-items";
import { listRefunds } from "@/lib/admin-data";
import { PAYMENT_METHOD_LABELS, REFUND_CAUSE_LABELS, REFUND_TABS } from "@/lib/admin-shared";
import { formatVnd } from "@/lib/format";
import { isAdmin, requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Hoàn tiền" };

export default function Page({ searchParams }: PageProps<"/admin/hoan-tien">) {
  return (
    <>
      <AdminHeader title="Hoàn tiền" />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <Refunds searchParams={searchParams} />
      </Suspense>
    </>
  );
}

/**
 * Đơn đã hủy nhưng khách đã bị trừ tiền online (B-33). Hệ thống không tự hoàn: chủ shop hoàn ở trang quản trị
 * VNPay/MoMo rồi đánh dấu ở đây. Nhân viên xem được, chỉ ADMIN đánh dấu.
 */
async function Refunds({ searchParams }: Pick<PageProps<"/admin/hoan-tien">, "searchParams">) {
  const sp = await searchParams;
  const tab = REFUND_TABS.find((t) => t.status === textParam(sp.tab).toUpperCase()) ?? REFUND_TABS[0];
  const page = pageParam(sp.trang);
  const session = await requireStaff("/admin/hoan-tien");
  const canComplete = isAdmin(session);
  const result = await listRefunds({ status: tab.status, page });

  return (
    <div className="flex flex-col gap-4">
      <p className="m-0 text-[13px] text-muted">
        Hoàn tiền ở trang quản trị của cổng thanh toán trước, rồi nhập mã giao dịch hoàn để đánh dấu.
        {canComplete ? "" : " Chỉ chủ shop (ADMIN) được đánh dấu đã hoàn."}
      </p>
      <div className="card overflow-hidden">
        <nav aria-label="Lọc theo trạng thái" className="flex gap-1 border-b border-admin-line p-3">
          {REFUND_TABS.map((t) => (
            <Link
              key={t.status}
              href={`/admin/hoan-tien?tab=${t.status.toLowerCase()}`}
              aria-current={t.status === tab.status ? "page" : undefined}
              className={`rounded-lg px-3 py-1.5 text-[13px] ${t.status === tab.status ? "bg-admin-line font-medium" : "text-muted hover:bg-admin-bg"}`}
            >
              {t.label}
            </Link>
          ))}
        </nav>
        {result.items.length === 0 ? (
          <p className="m-0 px-4 py-10 text-center text-[13px] text-muted">
            {tab.status === "PENDING" ? "Không có đơn nào cần hoàn tiền." : "Chưa có đơn nào đã hoàn."}
          </p>
        ) : (
          <ul className="m-0 flex list-none flex-col p-0">
            {result.items.map((r) => (
              <li key={r.id} className="flex flex-col gap-3 border-t border-admin-line p-4 text-[13px] first:border-t-0">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                  <Link href={`/admin/don-hang/${encodeURIComponent(r.orderCode)}`} className="font-medium hover:underline">
                    {r.orderCode}
                  </Link>
                  <span className="text-[15px] font-semibold">{formatVnd(r.amount)}</span>
                  <span className="text-muted">{PAYMENT_METHOD_LABELS[r.provider] ?? r.provider}</span>
                  <span className="ml-auto text-muted">{formatDateTime(r.createdAt)}</span>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <Badge tone={r.status === "PENDING" ? "critical" : "success"}>{REFUND_CAUSE_LABELS[r.cause] ?? r.cause}</Badge>
                  {r.providerTxnId ? <span className="text-muted">Mã giao dịch: {r.providerTxnId}</span> : null}
                  {r.cancelReason ? <span className="text-muted">Lý do hủy: {r.cancelReason}</span> : null}
                </div>
                {r.status === "DONE" ? (
                  <p className="m-0 text-muted">
                    Đã hoàn {r.completedAt ? formatDateTime(r.completedAt) : ""} · mã hoàn {r.reference}
                    {r.note ? ` · ${r.note}` : ""}
                  </p>
                ) : canComplete ? (
                  <RefundComplete refundId={r.id} version={r.version} />
                ) : null}
              </li>
            ))}
          </ul>
        )}
        <Pager basePath="/admin/hoan-tien" params={{ tab: tab.status.toLowerCase() }} page={page} hasNext={result.hasNext} total={result.total} />
      </div>
    </div>
  );
}
