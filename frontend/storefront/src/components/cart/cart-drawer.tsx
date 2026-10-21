"use client";

import Link from "next/link";
import { useEffect } from "react";
import { CartLines, CartNotices, FreeShipNote, useCartBlocked } from "@/components/cart/cart-lines";
import { CheckoutButton } from "@/components/cart/cart-page";
import { selectCount, selectSubtotal, useCartStore } from "@/lib/cart-store";
import { formatVnd } from "@/lib/format";

/** Ngăn kéo giỏ hàng trượt từ phải, rộng 440px, chỉ trên desktop (UI-10); mobile dùng trang /gio-hang. */
export function CartDrawer() {
  const open = useCartStore((s) => s.drawerOpen);
  const close = useCartStore((s) => s.closeDrawer);
  const count = useCartStore(selectCount);
  const subtotal = useCartStore(selectSubtotal);
  const blocked = useCartBlocked();

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") close();
    };
    document.addEventListener("keydown", onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = overflow;
    };
  }, [open, close]);

  return (
    <div className={`fixed inset-0 z-50 hidden lg:block ${open ? "" : "pointer-events-none"}`} inert={!open}>
      <div onClick={close} className={`absolute inset-0 bg-ink/30 transition-opacity duration-200 ${open ? "opacity-100" : "opacity-0"}`} />
      <aside
        role="dialog"
        aria-modal="true"
        aria-label="Giỏ hàng"
        className={`absolute right-0 top-0 flex h-full w-[440px] flex-col bg-paper transition-transform duration-300 ${open ? "translate-x-0" : "translate-x-full"}`}
      >
        <div className="flex h-20 flex-none items-center justify-between border-b border-line px-8">
          <span className="eyebrow">Giỏ hàng ({count})</span>
          <button type="button" onClick={close} aria-label="Đóng" className="-mr-2 inline-flex h-11 w-11 items-center justify-center">
            <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
              <path d="M5 5l14 14M19 5 5 19" />
            </svg>
          </button>
        </div>
        {count === 0 ? (
          <div className="flex flex-1 flex-col items-start gap-4 px-8 py-10">
            <CartNotices />
            <p className="m-0 text-[14px] text-muted">Giỏ hàng đang trống.</p>
            <Link href="/moi-ve" onClick={close} className="btn">
              Xem hàng mới về
            </Link>
          </div>
        ) : (
          <>
            <div className="flex flex-1 flex-col gap-2 overflow-y-auto px-8 py-4">
              <CartNotices />
              <FreeShipNote subtotal={subtotal} />
              <CartLines compact onNavigate={close} />
            </div>
            <div className="flex flex-none flex-col gap-2 border-t border-line px-8 py-6">
              <div className="flex items-baseline justify-between">
                <span className="text-[14px]">Tạm tính</span>
                <span className="font-display text-[24px] leading-none">{formatVnd(subtotal)}</span>
              </div>
              <span className="text-[12px] text-muted">Phí vận chuyển và voucher tính ở bước thanh toán.</span>
              <CheckoutButton blocked={blocked} onNavigate={close} />
              <Link href="/gio-hang" onClick={close} className="eyebrow ul mx-auto mt-2">
                Xem giỏ hàng
              </Link>
            </div>
          </>
        )}
      </aside>
    </div>
  );
}
