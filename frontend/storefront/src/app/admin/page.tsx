import type { Metadata } from "next";
import { Suspense } from "react";
import { LogoutButton } from "@/components/account/logout-button";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Quản trị", robots: { index: false } };

/** Khung trang quản trị; danh sách đơn, sản phẩm, tồn kho làm ở E5.7. */
export default function Page() {
  return (
    <main className="pad flex-1 py-12 lg:py-20">
      <span className="eyebrow text-muted">Quản trị</span>
      <Suspense fallback={<div className="mt-3 h-[80px]" />}>
        <AdminHome />
      </Suspense>
    </main>
  );
}

async function AdminHome() {
  const session = await requireStaff("/admin");
  return (
    <>
      <h1 className="font-display mt-3 text-[36px] font-medium leading-tight lg:text-[48px]">{session.user.name ?? session.user.email}</h1>
      <p className="mt-3 text-[14px] text-muted">Vai trò: {session.roles.filter((r) => r === "STAFF" || r === "ADMIN").join(", ")}</p>
      <div className="mt-10">
        <LogoutButton />
      </div>
    </>
  );
}
