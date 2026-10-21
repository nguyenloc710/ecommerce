import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { AdminHeader } from "@/components/admin/admin-ui";
import { listAdminOrders, listInventory } from "@/lib/admin-data";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Tổng quan" };

export default function Page() {
  return (
    <>
      <AdminHeader title="Tổng quan" />
      <Suspense fallback={<div className="card h-28 animate-pulse" />}>
        <Overview />
      </Suspense>
    </>
  );
}

/** Số việc cần làm hôm nay; mỗi ô dẫn tới danh sách đã lọc sẵn. */
async function Overview() {
  const session = await requireStaff("/admin");
  const [toProcess, packing, awaitingPayment, lowStock] = await Promise.all([
    listAdminOrders({ status: "CONFIRMED", page: 0, size: 1 }),
    listAdminOrders({ status: "PACKING", page: 0, size: 1 }),
    listAdminOrders({ status: "AWAITING_PAYMENT", page: 0, size: 1 }),
    listInventory({ lowStock: true, page: 0 }),
  ]);
  const tiles = [
    { label: "Đơn cần xử lý", value: toProcess.total, href: "/admin/don-hang?tab=can-xu-ly" },
    { label: "Đang đóng gói", value: packing.total, href: "/admin/don-hang?tab=dong-goi" },
    { label: "Chờ thanh toán", value: awaitingPayment.total, href: "/admin/don-hang?tab=cho-thanh-toan" },
    { label: "SKU sắp hết hàng", value: lowStock.total, href: "/admin/ton-kho?sap-het=1" },
  ];
  return (
    <div className="flex flex-col gap-4">
      <p className="m-0 text-[14px] text-muted">Xin chào {session.user.name ?? session.user.email}.</p>
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {tiles.map((t) => (
          <Link key={t.label} href={t.href} className="card flex flex-col gap-1 p-4 hover:border-ink">
            <span className="text-[13px] text-muted">{t.label}</span>
            <span className="text-[28px] font-semibold leading-tight">{t.value}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
