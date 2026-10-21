"use client";

import Image from "next/image";
import Link from "next/link";
import { useCartStore, type CartLine } from "@/lib/cart-store";
import { formatVnd } from "@/lib/format";

export const FREE_SHIP_FROM = 500_000; // B-07

/** Cảnh báo dưới từng dòng theo kết quả kiểm tra giá/tồn với server. */
function stockNote(l: CartLine): string | null {
  if (l.sellable === false) return "Hết hàng hoặc ngừng bán";
  if (l.available != null && l.qty > l.available) return `Chỉ còn ${l.available} sản phẩm`;
  return null;
}

const hasStockProblem = (l: CartLine) => stockNote(l) !== null;

/** Danh sách dòng giỏ dùng chung cho trang giỏ và ngăn kéo (UI-10). */
export function CartLines({ compact = false, onNavigate }: { compact?: boolean; onNavigate?: () => void }) {
  const lines = useCartStore((s) => s.lines);
  const setQty = useCartStore((s) => s.setQty);
  const remove = useCartStore((s) => s.remove);

  return (
    <ul className="m-0 list-none p-0">
      {lines.map((l) => {
        const note = stockNote(l);
        const atMax = l.available != null && l.qty >= l.available;
        return (
          <li key={`${l.skuId}:${l.engraving ?? ""}`} className={`flex gap-4 border-b border-line ${compact ? "py-4" : "py-5"}`}>
            <Link
              href={`/san-pham/${l.productSlug}`}
              onClick={onNavigate}
              className={`relative flex-none bg-stone ${compact ? "h-[104px] w-[78px]" : "h-[122px] w-[92px]"}`}
            >
              {l.imageUrl ? <Image src={l.imageUrl} alt={l.name} fill sizes="92px" className="object-cover" /> : null}
            </Link>
            <div className="flex min-w-0 flex-1 flex-col gap-1">
              <div className="flex justify-between gap-3">
                <Link href={`/san-pham/${l.productSlug}`} onClick={onNavigate} className="text-[13px]">
                  {l.name}
                </Link>
                <span className="flex-none text-[13px]">{formatVnd(l.price * l.qty)}</span>
              </div>
              <span className="text-[12px] text-muted">
                {l.variantLabel}
                {l.engraving ? ` · Khắc "${l.engraving}"` : ""}
              </span>
              {note ? <span className="text-[12px] text-sale">{note}</span> : null}
              <div className="mt-auto flex items-center justify-between pt-2">
                <div className="inline-flex h-9 items-center border border-line">
                  <button type="button" aria-label="Giảm" onClick={() => setQty(l.skuId, l.engraving, l.qty - 1)} className="h-full w-9">
                    −
                  </button>
                  <span className="min-w-6 text-center text-[13px]">{l.qty}</span>
                  <button
                    type="button"
                    aria-label="Tăng"
                    disabled={atMax || l.sellable === false}
                    onClick={() => setQty(l.skuId, l.engraving, l.qty + 1)}
                    className="h-full w-9 disabled:text-[#bbb]"
                  >
                    +
                  </button>
                </div>
                <button type="button" onClick={() => remove(l.skuId, l.engraving)} className="text-[12px] text-muted underline">
                  Xóa
                </button>
              </div>
            </div>
          </li>
        );
      })}
    </ul>
  );
}

/** Thông báo chung: lỗi cập nhật giỏ server, SKU ngừng bán không còn hiển thị được dưới dòng nào. */
export function CartNotices() {
  const error = useCartStore((s) => s.error);
  const issues = useCartStore((s) => s.issues);
  const lines = useCartStore((s) => s.lines);
  // Dòng đang có cảnh báo riêng thì không lặp lại ở đây
  const orphan = issues.filter((i) => !lines.some((l) => l.skuId === i.skuId && hasStockProblem(l)));
  if (!error && orphan.length === 0) return null;
  return (
    <div role="status" className="mb-2 flex flex-col gap-1 border border-sale px-4 py-2 text-[12px] text-sale">
      {error ? <span>{error}</span> : null}
      {orphan.map((i) => (
        <span key={i.skuId}>{i.message}</span>
      ))}
    </div>
  );
}

export function FreeShipNote({ subtotal }: { subtotal: number }) {
  return subtotal < FREE_SHIP_FROM ? (
    <p className="m-0 bg-bone px-4 py-2 text-[12px]">
      Mua thêm <strong>{formatVnd(FREE_SHIP_FROM - subtotal)}</strong> để được miễn phí vận chuyển.
    </p>
  ) : (
    <p className="m-0 bg-bone px-4 py-2 text-[12px]">
      Đơn của bạn được <strong>miễn phí vận chuyển</strong>.
    </p>
  );
}

/** Còn dòng hết hàng / vượt tồn thì chưa cho sang thanh toán. */
export function useCartBlocked(): boolean {
  return useCartStore((s) => s.lines.some(hasStockProblem));
}
