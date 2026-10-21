import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { LogoutButton } from "@/components/account/logout-button";
import { authFetch, isStaff, requireSession } from "@/lib/session";

export const metadata: Metadata = { title: "Tài khoản", robots: { index: false } };

/** Theo CustomerProfileView của customer-service. */
type CustomerProfile = {
  id: string;
  email: string;
  fullName: string | null;
  phone: string | null;
  createdAt: string;
};

export default function Page() {
  return (
    <main className="pad flex-1 py-12 lg:py-20">
      <span className="eyebrow text-muted">Tài khoản</span>
      <Suspense fallback={<div className="mt-3 h-[120px]" />}>
        <AccountOverview />
      </Suspense>
    </main>
  );
}

async function AccountOverview() {
  const session = await requireSession("/tai-khoan");
  // Lần gọi đầu sau đăng nhập sẽ tạo hồ sơ JIT ở customer-service
  const profile = await authFetch<CustomerProfile>("/api/customers/me");
  const since = new Intl.DateTimeFormat("vi-VN", { month: "long", year: "numeric" }).format(new Date(profile.createdAt));

  return (
    <>
      <h1 className="font-display mt-3 text-[36px] font-medium leading-tight lg:text-[48px]">
        Xin chào{profile.fullName ? `, ${profile.fullName}` : ""}
      </h1>
      <dl className="mt-8 grid max-w-[560px] grid-cols-[140px_1fr] gap-y-3 border-t border-line pt-6 text-[14px]">
        <dt className="text-muted">Email</dt>
        <dd className="m-0">{profile.email}</dd>
        <dt className="text-muted">Số điện thoại</dt>
        <dd className="m-0">{profile.phone ?? "Chưa có"}</dd>
        <dt className="text-muted">Thành viên từ</dt>
        <dd className="m-0">{since}</dd>
      </dl>
      <div className="mt-10 flex flex-wrap items-center gap-8">
        <Link href="/tai-khoan/don-hang" className="eyebrow ul">
          Đơn hàng của tôi
        </Link>
        {isStaff(session) && (
          <Link href="/admin" className="eyebrow ul">
            Trang quản trị
          </Link>
        )}
        <LogoutButton />
      </div>
    </>
  );
}
