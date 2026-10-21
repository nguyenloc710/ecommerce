import type { Metadata } from "next";
import { ProductCard } from "@/components/product/product-card";
import { listProducts, toCardData } from "@/lib/catalog";
import { MAX_PAGE_SIZE } from "@/lib/listing-params";

export const metadata: Metadata = { title: "Sale", description: "Sản phẩm đang giảm giá." };

/**
 * API chưa có bộ lọc "đang giảm giá" nên lọc phía server trên trang tối đa 96 sản phẩm mới nhất
 * (đủ cho catalog hiện tại; thêm tham số `onSale` vào API khi catalog lớn).
 */
export default async function SalePage() {
  const page = await listProducts({ options: {}, sort: "newest", size: MAX_PAGE_SIZE, view: "compact" });
  const onSale = page.items.filter((p) => p.compareAtPrice !== null && p.price !== null && p.compareAtPrice > p.price);
  return (
    <main className="flex flex-col pb-20">
      <div className="pad flex flex-col gap-3 pt-6 lg:pt-7">
        <span className="eyebrow text-sale">Ưu đãi</span>
        <h1 className="font-display text-[40px] font-medium leading-none lg:text-[56px]">Sale</h1>
        <p className="m-0 text-[14px] text-muted">{onSale.length} sản phẩm đang giảm giá.</p>
      </div>
      <section className="pad grid grid-cols-2 gap-x-4 gap-y-10 pt-8 lg:grid-cols-4">
        {onSale.map((p) => (
          <ProductCard key={p.id} product={toCardData(p)} />
        ))}
      </section>
    </main>
  );
}
