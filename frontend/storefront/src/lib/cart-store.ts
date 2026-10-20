"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";

/** Một dòng giỏ hàng của khách vãng lai (giữ ở trình duyệt; gộp vào giỏ server khi đăng nhập ở E5.5). */
export type CartLine = {
  skuId: string;
  productSlug: string;
  name: string;
  variantLabel: string;
  price: number;
  imageUrl?: string;
  qty: number;
  engraving?: string;
};

type CartState = {
  lines: CartLine[];
  hydrated: boolean;
  add: (line: Omit<CartLine, "qty">, qty: number) => void;
  setQty: (skuId: string, engraving: string | undefined, qty: number) => void;
  remove: (skuId: string, engraving?: string) => void;
  clear: () => void;
  setHydrated: () => void;
};

const sameLine = (a: CartLine, skuId: string, engraving?: string) => a.skuId === skuId && (a.engraving ?? "") === (engraving ?? "");

export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      lines: [],
      hydrated: false,
      add: (line, qty) =>
        set((s) => {
          const existing = s.lines.find((l) => sameLine(l, line.skuId, line.engraving));
          if (existing) {
            return { lines: s.lines.map((l) => (l === existing ? { ...l, qty: l.qty + qty } : l)) };
          }
          return { lines: [...s.lines, { ...line, qty }] };
        }),
      setQty: (skuId, engraving, qty) =>
        set((s) => ({
          lines: qty <= 0 ? s.lines.filter((l) => !sameLine(l, skuId, engraving)) : s.lines.map((l) => (sameLine(l, skuId, engraving) ? { ...l, qty } : l)),
        })),
      remove: (skuId, engraving) => set((s) => ({ lines: s.lines.filter((l) => !sameLine(l, skuId, engraving)) })),
      clear: () => set({ lines: [] }),
      setHydrated: () => set({ hydrated: true }),
    }),
    {
      name: "ecom-cart",
      partialize: (s) => ({ lines: s.lines }),
      // Không tự hydrate lúc import để HTML server và client khớp nhau; CartHydrator gọi rehydrate sau mount
      skipHydration: true,
      onRehydrateStorage: () => (state) => state?.setHydrated(),
    },
  ),
);

export const selectCount = (s: CartState) => s.lines.reduce((n, l) => n + l.qty, 0);
export const selectSubtotal = (s: CartState) => s.lines.reduce((n, l) => n + l.qty * l.price, 0);
