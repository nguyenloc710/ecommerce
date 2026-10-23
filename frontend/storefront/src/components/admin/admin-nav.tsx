"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Suspense } from "react";

const items = [
  { href: "/admin", label: "Tổng quan", exact: true },
  { href: "/admin/don-hang", label: "Đơn hàng" },
  { href: "/admin/san-pham", label: "Sản phẩm" },
  { href: "/admin/ton-kho", label: "Tồn kho" },
  { href: "/admin/danh-gia", label: "Đánh giá" },
  { href: "/admin/flash-sale", label: "Flash sale" },
  { href: "/admin/gia-vang", label: "Giá vàng" },
];

/** Menu trái trang quản trị; mục đang mở tô nền. Trên mobile thành hàng ngang cuộn được. */
export function AdminNav() {
  // usePathname ở route động cần Suspense (Cache Components); fallback là menu chưa tô mục đang mở
  return (
    <Suspense fallback={<NavLinks pathname={null} />}>
      <CurrentNav />
    </Suspense>
  );
}

function CurrentNav() {
  return <NavLinks pathname={usePathname()} />;
}

function NavLinks({ pathname }: { pathname: string | null }) {
  return (
    <nav aria-label="Quản trị" className="flex gap-1 overflow-x-auto lg:flex-col">
      {items.map((item) => {
        const active = pathname !== null && (item.exact ? pathname === item.href : pathname.startsWith(item.href));
        return (
          <Link
            key={item.href}
            href={item.href}
            aria-current={active ? "page" : undefined}
            className={`whitespace-nowrap rounded-lg px-3 py-2 text-[13px] font-medium ${active ? "bg-admin-line/70 text-ink" : "text-muted hover:bg-admin-line/40 hover:text-ink"}`}
          >
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
