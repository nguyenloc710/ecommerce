import type { Metadata } from "next";
import { Suspense } from "react";
import { AdminHeader } from "@/components/admin/admin-ui";
import { FlashSaleForm } from "@/components/admin/flash-sale-form";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Tạo flash sale" };

export default function Page() {
  return (
    <>
      <AdminHeader title="Tạo flash sale" back={{ href: "/admin/flash-sale", label: "Flash sale" }} />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <NewFlashSale />
      </Suspense>
    </>
  );
}

async function NewFlashSale() {
  await requireStaff("/admin/flash-sale/moi");
  return <FlashSaleForm />;
}
