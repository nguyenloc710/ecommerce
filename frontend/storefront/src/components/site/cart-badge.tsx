"use client";

import Link from "next/link";
import { useEffect } from "react";
import { selectCount, useCartStore } from "@/lib/cart-store";

/** Gọi rehydrate một lần sau mount (persist dùng skipHydration để tránh lệch HTML). */
export function CartHydrator() {
  useEffect(() => {
    void useCartStore.persist.rehydrate();
  }, []);
  return null;
}

export function CartLinkDesktop() {
  const count = useCartStore(selectCount);
  return (
    <Link href="/gio-hang" className="hidden text-[12px] font-medium uppercase tracking-[0.12em] hover:opacity-70 lg:inline">
      Giỏ hàng ({count})
    </Link>
  );
}

export function CartLinkMobile() {
  const count = useCartStore(selectCount);
  return (
    <Link href="/gio-hang" aria-label={`Giỏ hàng, ${count} sản phẩm`} className="relative -mr-2 inline-flex h-11 w-11 items-center justify-center lg:hidden">
      <svg aria-hidden="true" width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
        <path d="M6 7h12l-1 13H7L6 7Z" />
        <path d="M9 7a3 3 0 0 1 6 0" />
      </svg>
      {count > 0 ? (
        <span className="absolute right-1 top-1.5 inline-flex h-[15px] min-w-[15px] items-center justify-center rounded-full bg-ink text-[9px] font-medium text-paper">
          {count}
        </span>
      ) : null}
    </Link>
  );
}
