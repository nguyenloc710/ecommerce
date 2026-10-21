"use client";

import Link from "next/link";
import { CartLines, CartNotices, FREE_SHIP_FROM, FreeShipNote, useCartBlocked } from "@/components/cart/cart-lines";
import { selectCount, selectSubtotal, useCartStore } from "@/lib/cart-store";
import { formatVnd } from "@/lib/format";

/** Trang giỏ hàng (UI-10, bản trang; desktop còn có ngăn kéo). */
export function CartPage() {
  const hydrated = useCartStore((s) => s.hydrated);
  const empty = useCartStore((s) => s.lines.length === 0);
  const count = useCartStore(selectCount);
  const subtotal = useCartStore(selectSubtotal);
  const blocked = useCartBlocked();

  return (
    <main className="pad flex flex-col gap-6 pb-20 pt-6 lg:pt-8">
      <h1 className="font-display text-[34px] font-medium leading-none lg:text-[48px]">
        Giỏ hàng <span className="text-[16px] text-muted">({count})</span>
      </h1>

      {!hydrated ? (
        <div className="h-24 animate-pulse bg-stone" />
      ) : empty ? (
        <div className="flex flex-col items-start gap-4 py-10">
          <CartNotices />
          <p className="m-0 text-[14px] text-muted">Giỏ hàng đang trống.</p>
          <Link href="/moi-ve" className="btn">
            Xem hàng mới về
          </Link>
        </div>
      ) : (
        <div className="grid gap-10 lg:grid-cols-[1fr_380px]">
          <div className="flex flex-col gap-2">
            <CartNotices />
            <FreeShipNote subtotal={subtotal} />
            <CartLines />
          </div>

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
            <CheckoutButton blocked={blocked} />
            <span className="text-center text-[11px] uppercase tracking-[0.04em] text-muted">COD · VNPay · MoMo · Đổi trả 7 ngày</span>
          </aside>
        </div>
      )}
    </main>
  );
}

export function CheckoutButton({ blocked, onNavigate }: { blocked: boolean; onNavigate?: () => void }) {
  if (blocked) {
    return (
      <>
        <button type="button" disabled className="btn mt-2 w-full cursor-not-allowed opacity-40">
          Thanh toán
        </button>
        <span className="text-center text-[12px] text-sale">Vui lòng chỉnh các sản phẩm hết hàng trước khi thanh toán.</span>
      </>
    );
  }
  return (
    <Link href="/thanh-toan" onClick={onNavigate} className="btn mt-2 w-full">
      Thanh toán
    </Link>
  );
}
