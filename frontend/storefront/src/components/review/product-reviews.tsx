import Image from "next/image";
import { ReviewForm } from "@/components/review/review-form";
import { Stars } from "@/components/review/stars";
import type { ProductReviews as ProductReviewsData } from "@/lib/reviews-shared";

const dateFormat = new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric", timeZone: "Asia/Ho_Chi_Minh" });

/**
 * Khu "Đánh giá từ khách hàng" ở trang sản phẩm (E7.1): tóm tắt điểm, biểu đồ số sao, đánh giá đã duyệt và form gửi.
 * Dữ liệu đọc ở server (prerender + cache); form là client component tự kiểm tra quyền đánh giá.
 */
export function ProductReviews({ data, productSlug }: { data: ProductReviewsData; productSlug: string }) {
  const { averageRating, totalCount, countByRating, reviews } = data;
  return (
    <section id="danh-gia" aria-labelledby="danh-gia-title" className="pad scroll-mt-24 pt-20 lg:pt-24">
      <h2 id="danh-gia-title" className="font-display mb-8 text-[26px] font-medium lg:text-[40px]">
        Đánh giá từ khách hàng
      </h2>
      <div className="grid gap-12 lg:grid-cols-[320px_1fr] lg:gap-20">
        <div className="flex flex-col gap-6">
          {totalCount > 0 ? (
            <div className="flex flex-col gap-2">
              <div className="flex items-baseline gap-3">
                <span className="font-display text-[48px] leading-none">{averageRating.toLocaleString("vi-VN")}</span>
                <Stars value={averageRating} size={16} />
              </div>
              <span className="text-[13px] text-muted">{totalCount} đánh giá từ khách đã mua</span>
              <ul className="m-0 mt-3 flex list-none flex-col gap-1.5 p-0">
                {[5, 4, 3, 2, 1].map((star) => {
                  const count = countByRating[String(star)] ?? 0;
                  const pct = totalCount === 0 ? 0 : Math.round((count / totalCount) * 100);
                  return (
                    <li key={star} className="grid grid-cols-[28px_1fr_32px] items-center gap-3 text-[12px] text-muted">
                      <span>{star} ★</span>
                      <span className="h-[3px] bg-line" aria-hidden="true">
                        <span className="block h-full bg-ink" style={{ width: `${pct}%` }} />
                      </span>
                      <span className="text-right">{count}</span>
                    </li>
                  );
                })}
              </ul>
            </div>
          ) : (
            <p className="m-0 text-[14px] text-muted">Chưa có đánh giá. Khách đã mua có thể đánh giá sau khi nhận hàng.</p>
          )}
          <ReviewForm productId={data.productId} productSlug={productSlug} />
        </div>

        {reviews.items.length > 0 ? (
          <ul className="m-0 flex list-none flex-col p-0">
            {reviews.items.map((r) => (
              <li key={r.id} className="flex flex-col gap-2 border-t border-line py-6 first:border-t-0 first:pt-0">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                  <Stars value={r.rating} />
                  <span className="text-[13px]">{r.authorName}</span>
                  <span className="eyebrow text-[10px] text-ok">Đã mua hàng</span>
                  <span className="ml-auto text-[12px] text-muted">{dateFormat.format(new Date(r.createdAt))}</span>
                </div>
                {r.variantLabel ? <span className="text-[12px] text-muted">Phân loại: {r.variantLabel}</span> : null}
                {r.content ? <p className="m-0 whitespace-pre-line text-[14px] leading-[1.7] text-[#3a3a3a]">{r.content}</p> : null}
                {r.imageUrls.length > 0 ? (
                  <div className="flex flex-wrap gap-2 pt-1">
                    {r.imageUrls.map((url, i) => (
                      <a key={url} href={url} target="_blank" rel="noopener noreferrer" className="relative block h-20 w-20 bg-stone">
                        <Image src={url} alt={`Ảnh ${i + 1} của khách ${r.authorName}`} fill sizes="80px" className="object-cover" />
                      </a>
                    ))}
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </section>
  );
}
