"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { Countdown } from "@/components/flash/countdown";
import type { Availability, ProductDetail } from "@/lib/catalog-shared";
import { colorHex, optionLabel } from "@/lib/catalog-shared";
import { useCartStore } from "@/lib/cart-store";
import { formatVnd } from "@/lib/format";

type Props = {
  product: Pick<ProductDetail, "id" | "slug" | "name" | "options" | "variants" | "images" | "pricingType">;
  availability: Record<string, Availability>;
  showEngraving: boolean;
};

const ENGRAVING_MAX = 10; // B-08

const formatWeight = (g: number) => new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 3 }).format(g);
const formatRateTime = (iso: string) =>
  new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", timeZone: "Asia/Ho_Chi_Minh" }).format(new Date(iso));

/** Khung mua hàng dính bên phải (UI-09): chọn option, giá theo variant, tồn kho, khắc tên, thêm vào giỏ. */
export function BuyBox({ product, availability, showEngraving }: Props) {
  const variants = product.variants;
  const stockOf = (skuId: string) => availability[skuId];
  const sellable = (skuId: string) => stockOf(skuId)?.inStock ?? true; // chưa có dữ liệu kho → không chặn

  const firstSellable = variants.find((v) => sellable(v.id)) ?? variants[0];
  const [selected, setSelected] = useState<Record<string, string>>(firstSellable?.optionValues ?? {});
  const [qty, setQty] = useState(1);
  const [engraving, setEngraving] = useState("");
  const [added, setAdded] = useState(false);
  const add = useCartStore((s) => s.add);
  const openDrawer = useCartStore((s) => s.openDrawer);

  const variant = useMemo(
    () => variants.find((v) => product.options.every((o) => v.optionValues[o.name] === selected[o.name])),
    [variants, product.options, selected],
  );
  const stock = variant ? stockOf(variant.id) : undefined;
  const canBuy = Boolean(variant) && (variant ? sellable(variant.id) : false);
  const maxQty = stock?.remaining ?? 99;
  const price = variant?.price ?? Math.min(...variants.map((v) => v.price));
  const compareAt = variant?.compareAtPrice ?? null;

  /** Giá trị option còn mua được khi giữ nguyên các option khác đang chọn. */
  const isValueAvailable = (name: string, value: string) =>
    variants.some((v) => v.optionValues[name] === value && sellable(v.id) && product.options.every((o) => o.name === name || !selected[o.name] || v.optionValues[o.name] === selected[o.name]));

  const pick = (name: string, value: string) => {
    setSelected((s) => ({ ...s, [name]: value }));
    setQty(1);
    setAdded(false);
  };

  const addToCart = () => {
    if (!variant) return;
    const image = product.images.find((i) => i.variantId === variant.id)?.url ?? product.images[0]?.url;
    add(
      {
        skuId: variant.id,
        productSlug: product.slug,
        name: product.name,
        variantLabel: Object.values(variant.optionValues).join(" / "),
        price: variant.price,
        imageUrl: image,
        engraving: engraving.trim() || undefined,
      },
      qty,
    );
    setAdded(true);
    // Desktop mở ngăn kéo giỏ (UI-10); mobile giữ thông báo ngay dưới nút
    if (window.matchMedia("(min-width: 1024px)").matches) openDrawer();
  };

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-baseline gap-3">
        <span className="text-[18px]">{formatVnd(price)}</span>
        {compareAt && compareAt > price ? (
          <>
            <span className="text-[13px] text-muted line-through">{formatVnd(compareAt)}</span>
            <span className="eyebrow text-[10px] text-sale">-{Math.round((1 - price / compareAt) * 100)}%</span>
          </>
        ) : null}
        {product.pricingType === "GOLD_RATE" ? <span className="text-[12px] text-muted">· theo giá vàng ngày</span> : null}
      </div>
      {variant?.gold ? (
        <p className="-mt-2 m-0 text-[12px] leading-[1.6] text-muted">
          Vàng {variant.gold.karat.replace("K", "")}K {formatWeight(variant.gold.weightGram)} g × {formatVnd(variant.gold.ratePerGram)}/g + công {formatVnd(variant.gold.makingFee)}
          <br />
          Giá vàng cập nhật {formatRateTime(variant.gold.rateEffectiveAt)}, chốt giá khi đặt hàng
        </p>
      ) : null}
      {variant?.priceLabel === "FLASH_SALE" && variant.priceEndsAt ? (
        <div className="-mt-2 flex items-center gap-2 text-[12px] text-sale">
          <span className="eyebrow text-[10px]">Flash sale</span>
          <span>kết thúc sau</span>
          <Countdown endsAt={variant.priceEndsAt} />
        </div>
      ) : null}

      {product.options.map((option) => (
        <div key={option.name} className="flex flex-col gap-2.5">
          <div className="flex items-baseline justify-between">
            <span className="eyebrow">
              {optionLabel(option.name)}
              <span className="ml-1.5 text-[12px] font-normal normal-case tracking-normal text-muted">{selected[option.name]}</span>
            </span>
            {option.name === "size" ? (
              <Link href="/huong-dan-chon-size" className="ul text-[12px]">
                Hướng dẫn đo size
              </Link>
            ) : null}
          </div>
          <div className="flex flex-wrap gap-2">
            {option.values.map((value) => {
              const active = selected[option.name] === value;
              const available = isValueAvailable(option.name, value);
              if (option.name === "color") {
                return (
                  <button
                    key={value}
                    type="button"
                    aria-label={value}
                    aria-pressed={active}
                    onClick={() => pick(option.name, value)}
                    className={`h-8 w-8 rounded-full border-2 border-paper ${active ? "shadow-[0_0_0_1px_#111]" : "shadow-[0_0_0_1px_#ddd]"} ${available ? "" : "opacity-40"}`}
                    style={{ background: colorHex(value) }}
                  />
                );
              }
              return (
                <button
                  key={value}
                  type="button"
                  aria-pressed={active}
                  onClick={() => pick(option.name, value)}
                  className={`h-11 min-w-14 border px-3 text-[13px] ${active ? "border-ink bg-ink text-paper" : "border-line bg-paper hover:border-ink"} ${available ? "" : "text-[#bbb] line-through"}`}
                >
                  {value}
                </button>
              );
            })}
          </div>
        </div>
      ))}

      {variant && stock ? (
        stock.inStock ? (
          stock.lowStock ? (
            <span className="text-[12px] text-sale">Chỉ còn {stock.remaining} sản phẩm</span>
          ) : (
            <span className="text-[12px] text-muted">Còn hàng · Giao 2–4 ngày</span>
          )
        ) : (
          <span className="text-[12px] text-sale">Hết hàng</span>
        )
      ) : null}

      {showEngraving ? (
        <div className="flex flex-col gap-2">
          <label htmlFor="engraving" className="eyebrow">
            Khắc tên <span className="ml-1.5 text-[12px] font-normal normal-case tracking-normal text-muted">miễn phí, tối đa {ENGRAVING_MAX} ký tự</span>
          </label>
          <input id="engraving" value={engraving} onChange={(e) => setEngraving(e.target.value.slice(0, ENGRAVING_MAX))} maxLength={ENGRAVING_MAX} placeholder="VD: 14.02.2026" className="h-11 border-b border-ink bg-transparent text-[14px] outline-none" />
        </div>
      ) : null}

      <div className="flex flex-col gap-2.5 pt-1">
        <div className="flex gap-2.5">
          <div className="inline-flex h-13 flex-none items-center border border-line">
            <button type="button" aria-label="Giảm số lượng" onClick={() => setQty((q) => Math.max(1, q - 1))} className="h-full w-11 text-lg">−</button>
            <span className="min-w-7 text-center text-[14px]" aria-live="polite">{qty}</span>
            <button type="button" aria-label="Tăng số lượng" onClick={() => setQty((q) => Math.min(maxQty, q + 1))} className="h-full w-11 text-lg">+</button>
          </div>
          <button type="button" onClick={addToCart} disabled={!canBuy} className="btn flex-1 justify-between px-5 disabled:cursor-not-allowed disabled:opacity-40">
            <span>{canBuy ? "Thêm vào giỏ" : "Hết hàng"}</span>
            <span>{formatVnd(price * qty)}</span>
          </button>
        </div>
        {added ? (
          <div role="status" className="flex items-center justify-between gap-3 border border-ink px-4 py-3 text-[13px]">
            <span>Đã thêm {qty} sản phẩm vào giỏ.</span>
            <Link href="/gio-hang" className="eyebrow ul">
              Xem giỏ
            </Link>
          </div>
        ) : null}
      </div>
    </div>
  );
}
