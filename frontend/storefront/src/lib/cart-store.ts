"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import { refreshGuestCart, setServerLine, syncCart, type CartIssue, type CartLineInput, type CartResult, type SyncedCart } from "@/lib/cart-actions";

/**
 * Một dòng giỏ hàng. Khách vãng lai: giữ ở trình duyệt (localStorage). Khách đăng nhập: bản sao giỏ server,
 * không lưu xuống localStorage để không bị gộp lại lần hai.
 */
export type CartLine = {
  skuId: string;
  productSlug: string;
  name: string;
  variantLabel: string;
  price: number;
  imageUrl?: string;
  qty: number;
  engraving?: string;
  /** Có sau khi kiểm tra với server; null = còn nhiều. */
  available?: number | null;
  sellable?: boolean;
};

type Mode = "guest" | "server";

type CartState = {
  lines: CartLine[];
  hydrated: boolean;
  /** Đã đồng bộ với server xong lần đầu (biết chắc giỏ đang ở chế độ nào). */
  synced: boolean;
  mode: Mode;
  issues: CartIssue[];
  /** Lỗi gần nhất khi cập nhật giỏ server (hiển thị một dòng, kèm traceId). */
  error: string | null;
  drawerOpen: boolean;
  add: (line: Omit<CartLine, "qty">, qty: number) => void;
  setQty: (skuId: string, engraving: string | undefined, qty: number) => void;
  remove: (skuId: string, engraving?: string) => void;
  clear: () => void;
  openDrawer: () => void;
  closeDrawer: () => void;
  /** Gọi một lần sau khi đọc localStorage: gộp vào giỏ server nếu đã đăng nhập, không thì kiểm tra giá/tồn. */
  sync: () => Promise<void>;
};

const keyOf = (skuId: string, engraving?: string) => `${skuId}:${engraving ?? ""}`;
const sameLine = (a: CartLine, skuId: string, engraving?: string) => keyOf(a.skuId, a.engraving) === keyOf(skuId, engraving);
const toInput = (l: CartLine): CartLineInput => ({ skuId: l.skuId, productSlug: l.productSlug, quantity: l.qty, engraving: l.engraving });

/** Chỉ áp dụng kết quả của lần gọi server mới nhất, tránh response về trễ ghi đè thao tác sau. */
let requestSeq = 0;

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => {
      const applyServer = (cart: SyncedCart) => set({ mode: "server", lines: cart.lines, issues: cart.issues, error: null });

      /** Cập nhật lạc quan rồi đẩy lên server (chỉ ở chế độ server); lỗi thì lấy lại giỏ server làm chuẩn. */
      const pushLine = (skuId: string, productSlug: string, engraving: string | undefined, qty: number) => {
        if (get().mode !== "server") return;
        const seq = ++requestSeq;
        void setServerLine({ skuId, productSlug, quantity: qty, engraving }).then(async (res) => {
          if (seq !== requestSeq) return;
          if (res.ok) {
            applyServer(res.cart);
            return;
          }
          set({ error: res.traceId ? `${res.message} (mã lỗi ${res.traceId})` : res.message });
          const fresh = await syncCart([]);
          if (fresh?.ok && seq === requestSeq) set({ lines: fresh.cart.lines, issues: fresh.cart.issues });
        });
      };

      return {
        lines: [],
        hydrated: false,
        synced: false,
        mode: "guest",
        issues: [],
        error: null,
        drawerOpen: false,
        add: (line, qty) => {
          const existing = get().lines.find((l) => sameLine(l, line.skuId, line.engraving));
          const nextQty = (existing?.qty ?? 0) + qty;
          set((s) => ({
            lines: existing ? s.lines.map((l) => (l === existing ? { ...l, qty: nextQty } : l)) : [...s.lines, { ...line, qty }],
            error: null,
          }));
          pushLine(line.skuId, line.productSlug, line.engraving, nextQty);
        },
        setQty: (skuId, engraving, qty) => {
          const existing = get().lines.find((l) => sameLine(l, skuId, engraving));
          if (!existing) return;
          set((s) => ({
            lines: qty <= 0 ? s.lines.filter((l) => l !== existing) : s.lines.map((l) => (l === existing ? { ...l, qty } : l)),
            error: null,
          }));
          pushLine(skuId, existing.productSlug, engraving, Math.max(0, qty));
        },
        remove: (skuId, engraving) => get().setQty(skuId, engraving, 0),
        clear: () => set({ lines: [], issues: [] }),
        openDrawer: () => set({ drawerOpen: true }),
        closeDrawer: () => set({ drawerOpen: false }),
        sync: async () => {
          // Chế độ server: chỉ đọc lại giỏ server, không gửi dòng hiện có (sẽ bị merge cộng dồn lần nữa)
          const guest = get().mode === "guest" ? get().lines : [];
          const seq = ++requestSeq;
          const res: CartResult | null = await syncCart(guest.map(toInput)).catch(() => null);
          set({ synced: true });
          if (seq !== requestSeq) return;
          if (res?.ok) {
            applyServer(res.cart);
            return;
          }
          if (res && !res.ok) {
            // Đã đăng nhập nhưng order-service lỗi: giữ giỏ trình duyệt, lần tải sau gộp lại
            set({ error: res.message });
            return;
          }
          const refreshed = await refreshGuestCart(guest.map(toInput));
          if (!refreshed || seq !== requestSeq) return;
          set((s) => ({
            issues: refreshed.issues,
            lines: s.lines.map((l) => {
              const fresh = refreshed.lines.find((r) => sameLine(l, r.skuId, r.engraving));
              return fresh ? { ...l, ...fresh, qty: l.qty } : { ...l, available: 0, sellable: false };
            }),
          }));
        },
      };
    },
    {
      name: "ecom-cart",
      // Giỏ server không lưu xuống trình duyệt: lần tải sau lấy lại từ server, không gộp trùng
      partialize: (s) => ({ lines: s.mode === "guest" ? s.lines : [] }),
      // Không tự hydrate lúc import để HTML server và client khớp nhau; CartHydrator gọi rehydrate sau mount
      skipHydration: true,
      onRehydrateStorage: () => (state) => {
        useCartStore.setState({ hydrated: true });
        void state?.sync();
      },
    },
  ),
);

export const selectCount = (s: CartState) => s.lines.reduce((n, l) => n + l.qty, 0);
export const selectSubtotal = (s: CartState) => s.lines.reduce((n, l) => n + l.qty * l.price, 0);
