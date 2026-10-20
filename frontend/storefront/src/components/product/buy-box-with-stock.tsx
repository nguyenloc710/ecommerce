import { BuyBox } from "@/components/product/buy-box";
import { getAvailability, type ProductDetail } from "@/lib/catalog";

/** Đọc tồn kho lúc request (không cache) rồi giao cho BuyBox; đặt trong Suspense ở trang. */
export async function BuyBoxWithStock({ product, showEngraving }: { product: ProductDetail; showEngraving: boolean }) {
  const availability = await getAvailability(product.variants.map((v) => v.id));
  return <BuyBox product={product} availability={availability} showEngraving={showEngraving} />;
}

export function BuyBoxSkeleton() {
  return (
    <div className="flex flex-col gap-5" aria-busy="true">
      <div className="h-5 w-32 animate-pulse bg-stone" />
      <div className="h-8 w-40 animate-pulse bg-stone" />
      <div className="h-11 w-full animate-pulse bg-stone" />
      <div className="h-13 w-full animate-pulse bg-stone" />
    </div>
  );
}
