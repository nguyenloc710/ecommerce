import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { formatDateTime } from "@/components/order/order-items";
import { AdminHeader, Badge, Pager, pageParam, textParam } from "@/components/admin/admin-ui";
import { listAdminProducts } from "@/lib/admin-data";
import { PRODUCT_STATUS_LABELS, type ProductStatus } from "@/lib/admin-shared";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Sản phẩm" };

const STATUS_FILTERS: { value: string; label: string }[] = [
  { value: "", label: "Tất cả" },
  { value: "ACTIVE", label: "Đang bán" },
  { value: "DRAFT", label: "Nháp" },
  { value: "HIDDEN", label: "Đã ẩn" },
];

export default function Page({ searchParams }: PageProps<"/admin/san-pham">) {
  return (
    <>
      <AdminHeader
        title="Sản phẩm"
        actions={
          <Link href="/admin/san-pham/moi" className="btn-admin">
            Thêm sản phẩm
          </Link>
        }
      />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <Products searchParams={searchParams} />
      </Suspense>
    </>
  );
}

const statusTone = (s: ProductStatus) => (s === "ACTIVE" ? "success" : s === "DRAFT" ? "info" : "neutral");

async function Products({ searchParams }: Pick<PageProps<"/admin/san-pham">, "searchParams">) {
  const sp = await searchParams;
  const q = textParam(sp.q);
  const status = STATUS_FILTERS.some((f) => f.value === textParam(sp.trang_thai)) ? textParam(sp.trang_thai) : "";
  const page = pageParam(sp.trang);
  await requireStaff("/admin/san-pham");
  const result = await listAdminProducts({ q: q || undefined, status: status || undefined, page });

  return (
    <div className="card overflow-hidden">
      <form action="/admin/san-pham" className="flex flex-wrap gap-2 border-b border-admin-line p-3">
        <input name="q" defaultValue={q} placeholder="Tên hoặc slug" aria-label="Tìm sản phẩm" className="input-admin max-w-[280px]" />
        <select name="trang_thai" defaultValue={status} aria-label="Trạng thái" className="input-admin w-auto">
          {STATUS_FILTERS.map((f) => (
            <option key={f.value} value={f.value}>
              {f.label}
            </option>
          ))}
        </select>
        <button type="submit" className="btn-admin btn-admin-light">
          Lọc
        </button>
      </form>
      {result.items.length === 0 ? (
        <p className="m-0 px-4 py-10 text-center text-[13px] text-muted">Không có sản phẩm nào.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] border-collapse text-[13px]">
            <thead>
              <tr className="bg-admin-bg text-left text-[12px] text-muted">
                <th className="px-4 py-2 font-medium">Sản phẩm</th>
                <th className="px-4 py-2 font-medium">Danh mục</th>
                <th className="px-4 py-2 font-medium">Trạng thái</th>
                <th className="px-4 py-2 font-medium">Cập nhật</th>
              </tr>
            </thead>
            <tbody>
              {result.items.map((p) => (
                <tr key={p.id} className="border-t border-admin-line hover:bg-admin-bg">
                  <td className="px-4 py-3">
                    <Link href={`/admin/san-pham/${p.id}`} className="font-medium hover:underline">
                      {p.name}
                    </Link>
                    <div className="text-[12px] text-muted">/{p.slug}</div>
                  </td>
                  <td className="px-4 py-3">{p.categoryName}</td>
                  <td className="px-4 py-3">
                    <Badge tone={statusTone(p.status)}>{PRODUCT_STATUS_LABELS[p.status] ?? p.status}</Badge>
                  </td>
                  <td className="px-4 py-3 text-muted">{formatDateTime(p.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pager basePath="/admin/san-pham" params={{ q, trang_thai: status }} page={page} hasNext={result.hasNext} total={result.total} />
    </div>
  );
}
