import type { Metadata } from "next";
import { Suspense } from "react";
import { ProductListing } from "@/components/listing/product-listing";
import type { SearchParamsLike } from "@/lib/listing-params";

export const metadata: Metadata = { title: "Tìm kiếm" };

type Props = PageProps<"/tim-kiem">;

export default function SearchPage({ searchParams }: Props) {
  return (
    <Suspense fallback={<div className="pad pt-7"><div className="h-14 w-64 animate-pulse bg-stone" /></div>}>
      <SearchListing searchParams={searchParams} />
    </Suspense>
  );
}

async function SearchListing({ searchParams }: { searchParams: Promise<SearchParamsLike> }) {
  const sp = await searchParams;
  const q = (Array.isArray(sp.q) ? sp.q[0] : sp.q)?.trim() ?? "";
  return (
    <ProductListing
      title={q ? `“${q}”` : "Tìm kiếm"}
      subtitle={q ? undefined : "Gõ không dấu cũng được, ví dụ: nhan bac, ao linen."}
      breadcrumb={[{ name: "Tìm kiếm" }]}
      fixed={{ q: q || undefined }}
      basePath="/tim-kiem"
      searchParams={searchParams}
    >
      <form action="/tim-kiem" method="get" role="search" className="flex max-w-[560px] items-center gap-3 border-b border-ink pt-2">
        <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" />
        </svg>
        <label htmlFor="q" className="sr-only">Từ khóa</label>
        <input id="q" name="q" type="search" defaultValue={q} placeholder="Tìm áo, nhẫn, túi..." autoFocus={!q} className="h-12 w-full bg-transparent text-[16px] outline-none" />
        <button type="submit" className="eyebrow h-11 px-1">Tìm</button>
      </form>
    </ProductListing>
  );
}
