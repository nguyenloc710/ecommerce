import type { Metadata } from "next";
import { Suspense } from "react";
import { AdminHeader, Pager, pageParam, textParam } from "@/components/admin/admin-ui";
import { StockRow } from "@/components/admin/stock-row";
import { listInventory } from "@/lib/admin-data";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Tồn kho" };

export default function Page({ searchParams }: PageProps<"/admin/ton-kho">) {
  return (
    <>
      <AdminHeader title="Tồn kho" />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <Inventory searchParams={searchParams} />
      </Suspense>
    </>
  );
}

/** Danh sách SKU xếp theo số còn bán được tăng dần (sắp hết lên đầu). Đang giữ = khách đã đặt, chưa giao. */
async function Inventory({ searchParams }: Pick<PageProps<"/admin/ton-kho">, "searchParams">) {
  const sp = await searchParams;
  const q = textParam(sp.q);
  const lowStock = textParam(sp["sap-het"]) === "1";
  const page = pageParam(sp.trang);
  await requireStaff("/admin/ton-kho");
  const result = await listInventory({ q: q || undefined, lowStock, page });

  return (
    <div className="card overflow-hidden">
      <form action="/admin/ton-kho" className="flex flex-wrap items-center gap-3 border-b border-admin-line p-3">
        <input name="q" defaultValue={q} placeholder="Mã SKU hoặc tên sản phẩm" aria-label="Tìm SKU" className="input-admin max-w-[280px]" />
        <label className="flex items-center gap-2 text-[13px]">
          <input type="checkbox" name="sap-het" value="1" defaultChecked={lowStock} className="accent-ink" />
          Chỉ SKU sắp hết
        </label>
        <button type="submit" className="btn-admin btn-admin-light">
          Lọc
        </button>
      </form>
      {result.items.length === 0 ? (
        <p className="m-0 px-4 py-10 text-center text-[13px] text-muted">Không có SKU nào.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] border-collapse text-[13px]">
            <thead>
              <tr className="bg-admin-bg text-left text-[12px] text-muted">
                <th className="px-4 py-2 font-medium">Sản phẩm</th>
                <th className="px-4 py-2 font-medium">Thực có</th>
                <th className="px-4 py-2 font-medium">Đang giữ</th>
                <th className="px-4 py-2 font-medium">Còn bán được</th>
                <th className="px-4 py-2 font-medium">Ngưỡng sắp hết</th>
                <th className="px-4 py-2 font-medium" />
              </tr>
            </thead>
            <tbody>
              {result.items.map((r) => (
                <StockRow key={r.skuId} row={r} />
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pager basePath="/admin/ton-kho" params={{ q, "sap-het": lowStock ? "1" : undefined }} page={page} hasNext={result.hasNext} total={result.total} />
    </div>
  );
}
