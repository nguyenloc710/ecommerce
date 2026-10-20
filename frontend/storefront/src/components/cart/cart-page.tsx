"use client";

import Image from "next/image";
import Link from "next/link";
import { selectSubtotal, useCartStore } from "@/lib/cart-store";
import { formatVnd } from "@/lib/format";

const FREE_SHIP_FROM = 500_000; // B-07

/** Giỏ hàng khách vãng lai (UI-10, bản trang). Gộp giỏ server và thanh toán làm ở E5.5/E5.6. */
export function CartPage() {
  const { lines, hydrated, setQty, remove } = useCartStore();
  const subtotal = useCartStore(selectSubtotal);
  const count = lines.reduce((n, l) => n + l.qty, 0);

  return (
    <main className="pad flex flex-col gap-6 pb-20 pt-6 lg:pt-8">
      <h1 className="font-display text-[34px] font-medium leading-none lg:text-[48px]">
        Giỏ hàng <span className="text-[16px] text-muted">({count})</span>
      </h1>

      {!hydrated ? (
        <div className="h-24 animate-pulse bg-stone" />
      ) : lines.length === 0 ? (
        <div className="flex flex-col items-start gap-4 py-10">
          <p className="m-0 text-[14px] text-muted">Giỏ hàng đang trống.</p>
          <Link href="/moi-ve" className="btn">
            Xem hàng mới về
          </Link>
        </div>
      ) : (
        <div className="grid gap-10 lg:grid-cols-[1fr_380px]">
          <ul className="m-0 list-none p-0">
            {subtotal < FREE_SHIP_FROM ? (
              <li className="mb-2 bg-bone px-4 py-2 text-[12px]">
                Mua thêm <strong>{formatVnd(FREE_SHIP_FROM - subtotal)}</strong> để được miễn phí vận chuyển.
              </li>
            ) : (
              <li className="mb-2 bg-bone px-4 py-2 text-[12px]">Đơn của bạn được <strong>miễn phí vận chuyển</strong>.</li>
            )}
            {lines.map((l) => (
              <li key={`${l.skuId}:${l.engraving ?? ""}`} className="flex gap-4 border-b border-line py-5">
                <Link href={`/san-pham/${l.productSlug}`} className="relative h-[122px] w-[92px] flex-none bg-stone">
                  {l.imageUrl ? <Image src={l.imageUrl} alt={l.name} fill sizes="92px" className="object-cover" /> : null}
                </Link>
                <div className="flex min-w-0 flex-1 flex-col gap-1">
                  <div className="flex justify-between gap-3">
                    <Link href={`/san-pham/${l.productSlug}`} className="text-[13px]">
                      {l.name}
                    </Link>
                    <span className="flex-none text-[13px]">{formatVnd(l.price * l.qty)}</span>
                  </div>
                  <span className="text-[12px] text-muted">
                    {l.variantLabel}
                    {l.engraving ? ` · Khắc "${l.engraving}"` : ""}
                  </span>
                  <div className="mt-auto flex items-center justify-between">
                    <div className="inline-flex h-9 items-center border border-line">
                      <button type="button" aria-label="Giảm" onClick={() => setQty(l.skuId, l.engraving, l.qty - 1)} className="h-full w-9">−</button>
                      <span className="min-w-6 text-center text-[13px]">{l.qty}</span>
                      <button type="button" aria-label="Tăng" onClick={() => setQty(l.skuId, l.engraving, l.qty + 1)} className="h-full w-9">+</button>
                    </div>
                    <button type="button" onClick={() => remove(l.skuId, l.engraving)} className="text-[12px] text-muted underline">
                      Xóa
                    </button>
                  </div>
                </div>
              </li>
            ))}
          </ul>

          <aside className="flex h-fit flex-col gap-3 border border-line p-5 lg:sticky lg:top-24">
            <div className="flex justify-between text-[14px]">
              <span className="text-muted">Tạm tính</span>
              <span>{formatVnd(subtotal)}</span>
            </div>
            <div className="flex justify-between text-[14px]">
              <span className="text-muted">Vận chuyển</span>
              <span>{subtotal >= FREE_SHIP_FROM ? "Miễn phí" : "Tính ở bước sau"}</span>
            </div>
            <div className="flex items-baseline justify-between border-t border-line pt-3">
              <span>Tổng cộng</span>
              <span className="font-display text-[26px] leading-none">{formatVnd(subtotal)}</span>
            </div>
            <Link href="/thanh-toan" className="btn mt-2 w-full">
              Thanh toán
            </Link>
            <span className="text-center text-[11px] uppercase tracking-[0.04em] text-muted">COD · VNPay · MoMo · Đổi trả 7 ngày</span>
          </aside>
        </div>
      )}
    </main>
  );
}
