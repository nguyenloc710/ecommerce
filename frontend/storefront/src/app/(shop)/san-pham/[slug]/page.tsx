import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { BuyBoxSkeleton, BuyBoxWithStock } from "@/components/product/buy-box-with-stock";
import { Gallery } from "@/components/product/gallery";
import { ProductCard } from "@/components/product/product-card";
import { PLACEHOLDER_SLUG, getProduct, getRelatedProducts, iconFor, listAllProductSlugs, type ProductDetail } from "@/lib/catalog";
import { ProductJsonLd } from "@/components/product/product-json-ld";
import { ProductReviews } from "@/components/review/product-reviews";
import { Stars } from "@/components/review/stars";
import { getProductReviews } from "@/lib/reviews";

type Props = PageProps<"/san-pham/[slug]">;

/** Prerender sẵn mọi sản phẩm đang bán; slug mới thêm sau build được render lần đầu khi có người mở rồi cache. */
export async function generateStaticParams() {
  const slugs = await listAllProductSlugs();
  return (slugs.length > 0 ? slugs : [PLACEHOLDER_SLUG]).map((slug) => ({ slug }));
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const product = slug === PLACEHOLDER_SLUG ? null : await getProduct(slug);
  if (!product) return {};
  const description = product.description?.slice(0, 160) ?? `${product.name} chính hãng, giao toàn quốc.`;
  return {
    title: product.name,
    description,
    alternates: { canonical: `/san-pham/${product.slug}` },
    openGraph: { title: product.name, description, images: product.images.slice(0, 1).map((i) => ({ url: i.url, alt: i.alt ?? product.name })) },
  };
}

/**
 * Kiểm tra sản phẩm tồn tại TRƯỚC mọi <Suspense> để notFound() trả HTTP 404 thật (sau khi stream bắt đầu chỉ còn
 * được 200 + noindex). getProduct có cache nên lần kiểm tra này không tốn thêm request.
 */
export default async function ProductPage({ params }: Props) {
  const { slug } = await params;
  const product = slug === PLACEHOLDER_SLUG ? null : await getProduct(slug);
  if (!product) notFound();
  return <ProductDetailPage product={product} />;
}

async function ProductDetailPage({ product }: { product: ProductDetail }) {
  const isJewelry = product.breadcrumb.some((b) => b.slug === "trang-suc");
  const attributes = Object.entries(product.attributes).filter(([, v]) => typeof v === "string" || typeof v === "number");
  const [related, reviews] = await Promise.all([getRelatedProducts(product.category.slug, product.slug, 4), getProductReviews(product.id)]);

  return (
    <main className="flex flex-col pb-20">
      <ProductJsonLd product={product} rating={reviews.totalCount > 0 ? { average: reviews.averageRating, count: reviews.totalCount } : undefined} />
      <nav aria-label="Đường dẫn" className="pad flex gap-2 pt-5 text-[12px] text-muted lg:pt-6">
        <Link href="/">Trang chủ</Link>
        {product.breadcrumb.map((b) => (
          <span key={b.id} className="flex gap-2">
            <span>/</span>
            <Link href={`/danh-muc/${b.slug}`}>{b.name}</Link>
          </span>
        ))}
      </nav>

      <div className="grid items-start lg:grid-cols-[58fr_42fr]">
        <Gallery images={product.images} name={product.name} icon={iconFor(product.slug)} />

        <div className="flex flex-col gap-6 px-4 pt-6 lg:sticky lg:top-20 lg:px-12 lg:pt-10">
          <div className="flex flex-col gap-2.5">
            {isJewelry ? <span className="eyebrow text-gold">{String(product.attributes["Chất liệu"] ?? "Trang sức")}{product.attributes["Kiểm định"] ? " · Kèm giấy kiểm định" : ""}</span> : null}
            {product.brand ? <span className="eyebrow text-muted">{product.brand.name}</span> : null}
            <h1 className="font-display text-[28px] font-medium leading-[1.15] lg:text-[38px]">{product.name}</h1>
          </div>

          <Suspense fallback={<BuyBoxSkeleton />}>
            <BuyBoxWithStock product={product} showEngraving={isJewelry} />
          </Suspense>

          <ul className="m-0 flex list-none flex-col gap-1.5 p-0 text-[13px] text-[#3a3a3a]">
            <li>Miễn phí giao hàng cho đơn từ 500.000₫ · Giao 2–4 ngày</li>
            <li>Đổi trả trong 7 ngày</li>
            {isJewelry ? <li>Bảo hành đánh bóng trọn đời · Đổi size miễn phí 1 lần</li> : null}
          </ul>

          <div className="flex flex-col">
            <details className="group border-t border-line" open>
              <summary className="flex min-h-13 cursor-pointer list-none items-center justify-between text-[12px] font-medium uppercase tracking-[0.12em] [&::-webkit-details-marker]:hidden">
                Chi tiết
                <span aria-hidden="true" className="text-lg font-light group-open:hidden">+</span>
                <span aria-hidden="true" className="hidden text-lg font-light group-open:inline">–</span>
              </summary>
              <div className="pb-5 text-[14px] leading-[1.7] text-[#3a3a3a]">
                {product.description ? <p className="m-0 mb-3">{product.description}</p> : null}
                {attributes.length > 0 ? (
                  <dl className="grid grid-cols-[140px_1fr] gap-x-4 gap-y-1.5 text-[13px]">
                    {attributes.map(([k, v]) => (
                      <div key={k} className="contents">
                        <dt className="text-muted">{k}</dt>
                        <dd className="m-0">{String(v)}</dd>
                      </div>
                    ))}
                  </dl>
                ) : null}
              </div>
            </details>
            <details className="group border-t border-line">
              <summary className="flex min-h-13 cursor-pointer list-none items-center justify-between text-[12px] font-medium uppercase tracking-[0.12em] [&::-webkit-details-marker]:hidden">
                Giao hàng &amp; đổi trả
                <span aria-hidden="true" className="text-lg font-light group-open:hidden">+</span>
                <span aria-hidden="true" className="hidden text-lg font-light group-open:inline">–</span>
              </summary>
              <p className="m-0 pb-5 text-[14px] leading-[1.7] text-[#3a3a3a]">
                Giao toàn quốc qua GHN/GHTK trong 2–4 ngày làm việc. Đổi trả trong 7 ngày nếu sản phẩm còn nguyên tem mác, chưa qua sử dụng
                {isJewelry ? "; hàng đã khắc tên chỉ đổi khi lỗi từ shop" : ""}.
              </p>
            </details>
            <details className="group border-y border-line">
              <summary className="flex min-h-13 cursor-pointer list-none items-center justify-between text-[12px] font-medium uppercase tracking-[0.12em] [&::-webkit-details-marker]:hidden">
                {reviews.totalCount > 0 ? `Đánh giá (${reviews.totalCount})` : "Đánh giá"}
                <span aria-hidden="true" className="text-lg font-light group-open:hidden">+</span>
                <span aria-hidden="true" className="hidden text-lg font-light group-open:inline">–</span>
              </summary>
              <div className="flex items-center gap-3 pb-5 text-[14px]">
                {reviews.totalCount > 0 ? (
                  <>
                    <Stars value={reviews.averageRating} />
                    <span>{reviews.averageRating.toLocaleString("vi-VN")}/5</span>
                  </>
                ) : (
                  <span className="text-muted">Chưa có đánh giá.</span>
                )}
                <a href="#danh-gia" className="ul ml-auto text-[12px]">
                  Xem đánh giá
                </a>
              </div>
            </details>
          </div>
        </div>
      </div>

      <ProductReviews data={reviews} productSlug={product.slug} />

      {related.length > 0 ? (
        <section className="pad pt-20 lg:pt-24">
          <div className="mb-5 flex items-baseline justify-between lg:mb-7">
            <h2 className="font-display text-[26px] font-medium lg:text-[40px]">{isJewelry ? "Hoàn thiện bộ trang sức" : "Có thể bạn cũng thích"}</h2>
            <Link href={`/danh-muc/${product.category.slug}`} className="eyebrow ul">
              Xem thêm
            </Link>
          </div>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-4">
            {related.map((p) => (
              <ProductCard key={p.slug} product={p} />
            ))}
          </div>
        </section>
      ) : null}
    </main>
  );
}
