import type { Metadata } from "next";
import { Suspense } from "react";
import { AdminHeader, Pager, pageParam } from "@/components/admin/admin-ui";
import { GoldRateForm } from "@/components/admin/gold-rate-form";
import { formatDateTime } from "@/components/order/order-items";
import { currentGoldRates, listGoldRates } from "@/lib/admin-data";
import { GOLD_KARAT_LABELS, GOLD_KARATS } from "@/lib/admin-shared";
import { formatVnd } from "@/lib/format";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Giá vàng" };

export default function Page({ searchParams }: PageProps<"/admin/gia-vang">) {
  return (
    <>
      <AdminHeader title="Giá vàng" />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <GoldRates searchParams={searchParams} />
      </Suspense>
    </>
  );
}

/** Giá hiện hành theo loại vàng, form nhập giá mới và lịch sử (B-09). */
async function GoldRates({ searchParams }: Pick<PageProps<"/admin/gia-vang">, "searchParams">) {
  const page = pageParam((await searchParams).trang);
  await requireStaff("/admin/gia-vang");
  const [current, history] = await Promise.all([currentGoldRates(), listGoldRates(page)]);
  const byKarat = new Map(current.map((r) => [r.karat, r]));

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {GOLD_KARATS.map((k) => {
          const rate = byKarat.get(k);
          return (
            <div key={k} className="card flex flex-col gap-1 p-4">
              <span className="text-[12px] text-muted">{GOLD_KARAT_LABELS[k]}</span>
              <span className="text-[18px] font-semibold">{rate ? `${formatVnd(rate.pricePerGram)}/g` : "Chưa có giá"}</span>
              {rate ? <span className="text-[12px] text-muted">Cập nhật {formatDateTime(rate.effectiveAt)}</span> : null}
            </div>
          );
        })}
      </div>
      <GoldRateForm />
      <div className="card overflow-hidden">
        <h2 className="m-0 border-b border-admin-line px-4 py-3 text-[14px] font-semibold">Lịch sử</h2>
        {history.items.length === 0 ? (
          <p className="m-0 px-4 py-10 text-center text-[13px] text-muted">Chưa nhập giá vàng nào.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] border-collapse text-[13px]">
              <thead>
                <tr className="bg-admin-bg text-left text-[12px] text-muted">
                  <th className="px-4 py-2 font-medium">Thời điểm</th>
                  <th className="px-4 py-2 font-medium">Loại</th>
                  <th className="px-4 py-2 font-medium">Giá mỗi gam</th>
                  <th className="px-4 py-2 font-medium">Ghi chú</th>
                </tr>
              </thead>
              <tbody>
                {history.items.map((r) => (
                  <tr key={r.id} className="border-t border-admin-line">
                    <td className="px-4 py-2.5 text-muted">{formatDateTime(r.effectiveAt)}</td>
                    <td className="px-4 py-2.5">{GOLD_KARAT_LABELS[r.karat]}</td>
                    <td className="px-4 py-2.5">{formatVnd(r.pricePerGram)}</td>
                    <td className="px-4 py-2.5 text-muted">{r.note ?? ""}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pager basePath="/admin/gia-vang" params={{}} page={page} hasNext={history.hasNext} total={history.total} />
      </div>
    </div>
  );
}
