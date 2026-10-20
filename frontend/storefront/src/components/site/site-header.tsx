import Link from "next/link";
import { CartHydrator, CartLinkDesktop, CartLinkMobile } from "@/components/site/cart-badge";

const shopName = process.env.NEXT_PUBLIC_SHOP_NAME ?? "Tên shop";

const nav = [
  { href: "/danh-muc/thoi-trang", label: "Thời trang" },
  { href: "/danh-muc/phu-kien", label: "Phụ kiện" },
  { href: "/danh-muc/trang-suc", label: "Trang sức" },
  { href: "/moi-ve", label: "Mới về" },
  { href: "/sale", label: "Sale", sale: true },
];

/** Header theo UI-06: menu trái, logo serif giữa, tiện ích phải; thanh thông báo phía trên. */
export function SiteHeader() {
  return (
    <>
      <CartHydrator />
      <div className="bg-ink px-4 py-2 text-center text-[11px] tracking-[0.08em] text-paper">
        MIỄN PHÍ GIAO HÀNG CHO ĐƠN TỪ 500.000₫ · ĐỔI TRẢ 7 NGÀY
      </div>
      <header className="pad sticky top-0 z-30 grid h-14 grid-cols-[1fr_auto_1fr] items-center border-b border-line bg-paper lg:h-20">
        <div className="flex items-center gap-7">
          <button type="button" aria-label="Menu" className="-ml-2 inline-flex h-11 w-11 items-center justify-center lg:hidden">
            <svg aria-hidden="true" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
              <path d="M4 7h16M4 12h16M4 17h16" />
            </svg>
          </button>
          <nav aria-label="Danh mục chính" className="hidden gap-7 lg:flex">
            {nav.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={`text-[12px] font-medium uppercase tracking-[0.12em] hover:opacity-70 ${item.sale ? "text-sale" : ""}`}
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </div>
        <Link href="/" className="font-display pl-[0.22em] text-[22px] font-semibold uppercase tracking-[0.22em] lg:text-[30px]">
          {shopName}
        </Link>
        <div className="flex items-center justify-end gap-5">
          <Link href="/tim-kiem" className="hidden items-center gap-2 text-[12px] font-medium uppercase tracking-[0.12em] hover:opacity-70 lg:inline-flex">
            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
            Tìm kiếm
          </Link>
          <Link href="/tim-kiem" aria-label="Tìm kiếm" className="inline-flex h-11 w-11 items-center justify-center lg:hidden">
            <svg aria-hidden="true" width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
          </Link>
          <Link href="/tai-khoan" className="hidden text-[12px] font-medium uppercase tracking-[0.12em] hover:opacity-70 lg:inline">
            Tài khoản
          </Link>
          <CartLinkDesktop />
          <CartLinkMobile />
        </div>
      </header>
    </>
  );
}
