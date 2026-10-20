import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense, type ReactNode } from "react";
import { ApiError } from "@/lib/api";
import { getFacets, listProducts, toCardData } from "@/lib/catalog";
import { PAGE_SIZE, MAX_PAGE_SIZE, buildPageQuery, parseListingParams, type ListingParams, type SearchParamsLike } from "@/lib/listing-params";
import { FilterBar } from "@/components/listing/filter-bar";
import { ProductCard } from "@/components/product/product-card";

type Fixed = Partial<Pick<ListingParams, "category" | "brand" | "q">>;

type Props = {
  title: string;
  subtitle?: string;
  breadcrumb: { name: string; href?: string }[];
  /** Chip danh mục con hoặc ô tìm kiếm, hiển thị dưới tiêu đề */
  children?: ReactNode;
  fixed: Fixed;
  basePath: string;
  searchParams: Promise<SearchParamsLike>;
};

/**
 * Trang danh sách (UI-08): tiêu đề serif, thanh lọc dính, lưới không viền, "Đang hiển thị n/N + Xem thêm".
 * Phần đọc searchParams nằm trong Suspense để tiêu đề vẫn được prerender.
 */
export function ProductListing({ title, subtitle, breadcrumb, children, fixed, basePath, searchParams }: Props) {
  return (
    <main className="flex flex-col pb-20">
      <div className="pad flex flex-col gap-3 pt-6 lg:pt-7">
        <nav aria-label="Đường dẫn" className="flex gap-2 text-[12px] text-muted">
          <Link href="/">Trang chủ</Link>
          {breadcrumb.map((b) => (
            <span key={b.name} className="flex gap-2">
              <span>/</span>
              {b.href ? <Link href={b.href}>{b.name}</Link> : <span className="text-ink">{b.name}</span>}
            </span>
          ))}
        </nav>
        <h1 className="font-display text-[40px] font-medium leading-none lg:text-[56px]">{title}</h1>
        {subtitle ? <p className="m-0 max-w-[560px] text-[14px] text-muted">{subtitle}</p> : null}
        {children}
      </div>
      <Suspense fallback={<div className="mt-7 h-12 border-y border-line lg:h-14" />}>
        <FilterBarSection fixed={fixed} basePath={basePath} searchParams={searchParams} />
      </Suspense>
      <Suspense fallback={<GridSkeleton />}>
        <GridSection fixed={fixed} basePath={basePath} searchParams={searchParams} />
      </Suspense>
    </main>
  );
}

async function FilterBarSection({ fixed, basePath, searchParams }: Pick<Props, "fixed" | "basePath" | "searchParams">) {
  const params = parseListingParams(await searchParams, fixed);
  const [facets, page] = await Promise.all([getFacets(params.category, params.brand, params.q), safeList(params)]);
  return (
    <div className="mt-7">
      <FilterBar facets={facets} params={params} basePath={basePath} total={page?.total ?? 0} />
    </div>
  );
}

async function GridSection({ fixed, basePath, searchParams }: Pick<Props, "fixed" | "basePath" | "searchParams">) {
  const params = parseListingParams(await searchParams, fixed);
  const page = await safeList(params);
  if (!page) notFound();
  const cols = params.view === "wide" ? "grid-cols-1 lg:grid-cols-2" : "grid-cols-2 lg:grid-cols-4";
  const shown = page.items.length;
  return (
    <>
      {shown === 0 ? (
        <div className="pad py-20 text-center">
          <p className="font-display text-[28px]">Không có sản phẩm phù hợp</p>
          <p className="mt-2 text-[14px] text-muted">Thử bỏ bớt bộ lọc hoặc dùng từ khóa khác.</p>
        </div>
      ) : (
        <section className={`pad grid gap-x-4 gap-y-10 pt-7 ${cols}`}>
          {page.items.map((p) => (
            <ProductCard key={p.id} product={toCardData(p)} />
          ))}
        </section>
      )}
      {page.total > 0 ? (
        <div className="pad flex flex-col items-center gap-4 pt-16">
          <span className="text-[12px] uppercase tracking-[0.08em] text-muted">
            Đang hiển thị {shown} / {page.total}
          </span>
          <div className="h-0.5 w-[200px] bg-line">
            <div className="h-full bg-ink" style={{ width: `${Math.min(100, Math.round((shown / page.total) * 100))}%` }} />
          </div>
          {page.hasNext && params.size < MAX_PAGE_SIZE ? (
            <Link href={`${basePath}${buildPageQuery(params, { size: Math.min(MAX_PAGE_SIZE, params.size + PAGE_SIZE) })}`} scroll={false} className="btn btn-light min-h-12">
              Xem thêm
            </Link>
          ) : null}
        </div>
      ) : null}
    </>
  );
}

/** 404 từ API (slug danh mục/thương hiệu sai) → null để GridSection gọi notFound(). */
async function safeList(params: ListingParams) {
  try {
    return await listProducts(params);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

function GridSkeleton() {
  return (
    <section className="pad grid grid-cols-2 gap-x-4 gap-y-10 pt-7 lg:grid-cols-4" aria-busy="true" aria-label="Đang tải sản phẩm">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="flex flex-col gap-2">
          <div className="aspect-[3/4] animate-pulse bg-stone" />
          <div className="h-3 w-3/4 animate-pulse bg-stone" />
          <div className="h-3 w-1/3 animate-pulse bg-stone" />
        </div>
      ))}
    </section>
  );
}
