import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { Suspense } from "react";
import { AdminHeader, Badge, Pager, pageParam, textParam } from "@/components/admin/admin-ui";
import { ReviewModeration } from "@/components/admin/review-actions";
import { formatDateTime } from "@/components/order/order-items";
import { Stars } from "@/components/review/stars";
import { listReviews } from "@/lib/admin-data";
import { REVIEW_TABS } from "@/lib/admin-shared";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Đánh giá" };

export default function Page({ searchParams }: PageProps<"/admin/danh-gia">) {
  return (
    <>
      <AdminHeader title="Đánh giá" />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <Reviews searchParams={searchParams} />
      </Suspense>
    </>
  );
}

/** Kiểm duyệt đánh giá (B-10): chờ duyệt xếp cũ nhất trước để xử lý theo thứ tự. */
async function Reviews({ searchParams }: Pick<PageProps<"/admin/danh-gia">, "searchParams">) {
  const sp = await searchParams;
  const tab = REVIEW_TABS.find((t) => t.status === textParam(sp.tab).toUpperCase()) ?? REVIEW_TABS[0];
  const page = pageParam(sp.trang);
  await requireStaff("/admin/danh-gia");
  const result = await listReviews({ status: tab.status, page });

  return (
    <div className="card overflow-hidden">
      <nav aria-label="Lọc theo trạng thái" className="flex gap-1 border-b border-admin-line p-3">
        {REVIEW_TABS.map((t) => (
          <Link
            key={t.status}
            href={`/admin/danh-gia?tab=${t.status.toLowerCase()}`}
            aria-current={t.status === tab.status ? "page" : undefined}
            className={`rounded-lg px-3 py-1.5 text-[13px] ${t.status === tab.status ? "bg-admin-line font-medium" : "text-muted hover:bg-admin-bg"}`}
          >
            {t.label}
          </Link>
        ))}
      </nav>
      {result.items.length === 0 ? (
        <p className="m-0 px-4 py-10 text-center text-[13px] text-muted">Không có đánh giá nào.</p>
      ) : (
        <ul className="m-0 flex list-none flex-col p-0">
          {result.items.map((r) => (
            <li key={r.id} className="flex flex-col gap-3 border-t border-admin-line p-4 text-[13px] first:border-t-0">
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                <Stars value={r.rating} />
                <span className="font-medium">{r.authorName}</span>
                <span className="text-muted">
                  {r.productSlug ? (
                    <Link href={`/san-pham/${r.productSlug}`} target="_blank" className="hover:underline">
                      {r.productName}
                    </Link>
                  ) : (
                    r.productName
                  )}
                  {r.variantLabel ? ` · ${r.variantLabel}` : ""}
                </span>
                <span className="ml-auto text-muted">{formatDateTime(r.createdAt)}</span>
              </div>
              {r.content ? <p className="m-0 whitespace-pre-line">{r.content}</p> : <p className="m-0 text-muted">(Không có nội dung)</p>}
              {r.imageUrls.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {r.imageUrls.map((url, i) => (
                    <a key={url} href={url} target="_blank" rel="noopener noreferrer" className="relative block h-20 w-20 overflow-hidden rounded-md bg-admin-bg">
                      <Image src={url} alt={`Ảnh ${i + 1}`} fill sizes="80px" className="object-cover" />
                    </a>
                  ))}
                </div>
              ) : null}
              {r.rejectReason ? (
                <div>
                  <Badge tone="critical">Lý do: {r.rejectReason}</Badge>
                </div>
              ) : null}
              <ReviewModeration reviewId={r.id} status={r.status} />
            </li>
          ))}
        </ul>
      )}
      <Pager basePath="/admin/danh-gia" params={{ tab: tab.status.toLowerCase() }} page={page} hasNext={result.hasNext} total={result.total} />
    </div>
  );
}
