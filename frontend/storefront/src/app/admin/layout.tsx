import type { Metadata } from "next";
import Link from "next/link";
import { AdminNav } from "@/components/admin/admin-nav";
import { logout } from "@/lib/auth-actions";

const shopName = process.env.NEXT_PUBLIC_SHOP_NAME ?? "Tên shop";

export const metadata: Metadata = { title: { default: "Quản trị", template: `%s · Quản trị | ${shopName}` }, robots: { index: false } };

/**
 * Khung trang quản trị (UI-12, kiểu Shopify admin): nền xám nhạt, sidebar sáng, nội dung dạng thẻ.
 * Quyền STAFF/ADMIN chặn ở proxy, từng trang gọi lại requireStaff khi đọc dữ liệu.
 */
export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    <div className="flex min-h-screen flex-1 flex-col bg-admin-bg lg:flex-row">
      <aside className="flex flex-none flex-col gap-4 border-b border-admin-line bg-paper px-4 py-4 lg:sticky lg:top-0 lg:h-screen lg:w-[232px] lg:border-b-0 lg:border-r lg:py-6">
        <div className="flex items-center justify-between lg:flex-col lg:items-start lg:gap-1">
          <Link href="/admin" className="font-display text-[20px] font-semibold uppercase tracking-[0.18em]">
            {shopName}
          </Link>
          <span className="text-[11px] text-muted">Quản trị cửa hàng</span>
        </div>
        <AdminNav />
        <div className="mt-auto hidden flex-col gap-2 text-[13px] lg:flex">
          <Link href="/" className="text-muted hover:text-ink">
            Xem cửa hàng ↗
          </Link>
          <form action={logout}>
            <button type="submit" className="cursor-pointer text-muted hover:text-ink">
              Đăng xuất
            </button>
          </form>
        </div>
      </aside>
      <main className="min-w-0 flex-1 px-4 py-6 lg:px-8 lg:py-8">{children}</main>
    </div>
  );
}
