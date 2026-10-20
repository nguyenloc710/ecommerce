import Link from "next/link";
import { formatVnd } from "@/lib/format";
import { PlaceholderImage, type IconName, type Tone } from "@/components/ui/placeholder-image";

export type ProductCardData = {
  slug: string;
  name: string;
  price: number;
  compareAtPrice?: number;
  /** Nhãn nhỏ góc trên trái ảnh: "Mới", "Bán chạy", "-20%", "Hết hàng" (UI-07) */
  label?: string;
  labelTone?: "default" | "sale" | "gold";
  colors?: string[];
  imageUrl?: string;
  tone?: Tone;
  icon?: IconName;
  soldOut?: boolean;
};

const labelClass = {
  default: "text-ink",
  sale: "text-sale",
  gold: "text-gold",
};

export function ProductCard({ product }: { product: ProductCardData }) {
  const href = `/san-pham/${product.slug}`;
  return (
    <div className={`flex min-w-0 flex-col gap-2 ${product.soldOut ? "opacity-55" : ""}`}>
      <Link href={href} className="block">
        <PlaceholderImage tone={product.tone} icon={product.icon} className="aspect-[3/4]">
          {product.label ? (
            <span className={`eyebrow absolute left-3 top-3 text-[10px] ${labelClass[product.labelTone ?? "default"]}`}>
              {product.label}
            </span>
          ) : null}
        </PlaceholderImage>
      </Link>
      <Link href={href} className="text-[13px] hover:opacity-70">
        {product.name}
      </Link>
      <span className="text-[13px]">
        {product.compareAtPrice ? (
          <>
            <span className="text-sale">{formatVnd(product.price)}</span>
            <span className="ml-1.5 text-muted line-through">{formatVnd(product.compareAtPrice)}</span>
          </>
        ) : (
          formatVnd(product.price)
        )}
      </span>
      {product.colors?.length ? (
        <div className="flex items-center gap-1.5" aria-label={`${product.colors.length} màu`}>
          {product.colors.map((c) => (
            <i key={c} className="inline-block h-[9px] w-[9px] rounded-full border border-black/10" style={{ background: c }} />
          ))}
        </div>
      ) : null}
    </div>
  );
}
