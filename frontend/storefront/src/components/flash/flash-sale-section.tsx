import Link from "next/link";
import { Countdown } from "@/components/flash/countdown";
import { CoverImage } from "@/components/ui/cover-image";
import { formatVnd } from "@/lib/format";
import { getCurrentFlashSales } from "@/lib/flash-sale";

/** Khối flash sale ở trang chủ: chương trình sắp kết thúc nhất, đếm ngược và thanh "đã bán". Không có thì không render. */
export async function FlashSaleSection() {
  const sale = (await getCurrentFlashSales())[0];
  if (!sale) return null;
  return (
    <section className="pad pt-8 lg:pt-24" aria-labelledby="flash-sale-title">
      <div className="mb-3 flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2 lg:mb-7">
        <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
          <h2 id="flash-sale-title" className="font-display text-[28px] font-medium lg:text-[44px]">
            Flash sale
          </h2>
          <span className="text-[13px] text-muted">{sale.name}</span>
        </div>
        <span className="flex items-baseline gap-2 text-[13px] text-sale">
          <span className="eyebrow text-[10px]">Kết thúc sau</span>
          <Countdown endsAt={sale.endsAt} className="text-[15px]" />
        </span>
      </div>
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-6">
        {sale.items.map((item) => (
          <Link key={item.skuId} href={`/san-pham/${item.productSlug}`} className={`flex min-w-0 flex-col gap-2 ${item.soldOut ? "opacity-55" : ""}`}>
            <CoverImage src={item.imageUrl ?? undefined} alt={item.productName} className="aspect-[3/4]" sizes="(min-width: 1024px) 25vw, 50vw">
              <span className="eyebrow absolute left-3 top-3 z-20 bg-paper/85 px-1.5 py-0.5 text-[10px] text-sale">
                -{Math.round((1 - item.salePrice / item.compareAtPrice) * 100)}%
              </span>
            </CoverImage>
            <span className="text-[13px]">
              {item.productName}
              <span className="text-muted"> · {item.variantLabel}</span>
            </span>
            <span className="text-[13px]">
              <span className="text-sale">{formatVnd(item.salePrice)}</span>
              <span className="ml-1.5 text-muted line-through">{formatVnd(item.compareAtPrice)}</span>
            </span>
            <span className="flex flex-col gap-1">
              <span className="h-1 w-full bg-stone" aria-hidden="true">
                <span className="block h-full bg-sale" style={{ width: `${item.claimedPercent}%` }} />
              </span>
              <span className="text-[11px] text-muted">{item.soldOut ? "Đã hết suất" : `Đã bán ${item.claimedPercent}%`}</span>
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}
