import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { AdminHeader, Badge, Pager, pageParam } from "@/components/admin/admin-ui";
import { formatDateTime } from "@/components/order/order-items";
import { listFlashSales } from "@/lib/admin-data";
import { PHASE_TEXT, PHASE_TONE } from "@/lib/flash-sale-shared";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Flash sale" };

export default function Page({ searchParams }: PageProps<"/admin/flash-sale">) {
  return (
    <>
      <AdminHeader
        title="Flash sale"
        actions={
          <Link href="/admin/flash-sale/moi" className="btn-admin">
            Tạo chương trình
          </Link>
        }
      />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <FlashSales searchParams={searchParams} />
      </Suspense>
    </>
  );
}

async function FlashSales({ searchParams }: Pick<PageProps<"/admin/flash-sale">, "searchParams">) {
  const page = pageParam((await searchParams).trang);
  await requireStaff("/admin/flash-sale");
  const result = await listFlashSales(page);

  return (
    <div className="card overflow-hidden">
      {result.items.length === 0 ? (
        <p className="m-0 px-4 py-10 text-center text-[13px] text-muted">Chưa có chương trình nào.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] border-collapse text-[13px]">
            <thead>
              <tr className="bg-admin-bg text-left text-[12px] text-muted">
                <th className="px-4 py-2 font-medium">Chương trình</th>
                <th className="px-4 py-2 font-medium">Thời gian</th>
                <th className="px-4 py-2 font-medium">Trạng thái</th>
                <th className="px-4 py-2 font-medium">SKU</th>
                <th className="px-4 py-2 font-medium">Đã bán / suất</th>
              </tr>
            </thead>
            <tbody>
              {result.items.map((s) => (
                <tr key={s.id} className="border-t border-admin-line">
                  <td className="px-4 py-2.5">
                    <Link href={`/admin/flash-sale/${s.id}`} className="font-medium hover:underline">
                      {s.name}
                    </Link>
                  </td>
                  <td className="px-4 py-2.5 text-muted">
                    {formatDateTime(s.startsAt)} → {formatDateTime(s.endsAt)}
                  </td>
                  <td className="px-4 py-2.5">
                    <Badge tone={PHASE_TONE[s.phase]}>{PHASE_TEXT[s.phase]}</Badge>
                  </td>
                  <td className="px-4 py-2.5">{s.itemCount}</td>
                  <td className="px-4 py-2.5">
                    {s.totalSold} / {s.totalQuota}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pager basePath="/admin/flash-sale" params={{}} page={page} hasNext={result.hasNext} total={result.total} />
    </div>
  );
}
