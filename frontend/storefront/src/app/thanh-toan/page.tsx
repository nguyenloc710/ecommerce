import type { Metadata } from "next";
import { Suspense } from "react";
import { CheckoutForm } from "@/components/checkout/checkout-form";
import type { SavedAddress } from "@/lib/checkout-shared";
import { getProvinces } from "@/lib/divisions";
import { authFetch, requireSession } from "@/lib/session";

export const metadata: Metadata = { title: "Thanh toán", robots: { index: false } };

export default function Page() {
  return (
    <main className="pad flex-1 pb-20 pt-6 lg:pt-10">
      <h1 className="font-display mb-8 text-[34px] font-medium leading-none lg:mb-12 lg:text-[48px]">Thanh toán</h1>
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone" />}>
        <CheckoutLoader />
      </Suspense>
    </main>
  );
}

async function CheckoutLoader() {
  const session = await requireSession("/thanh-toan");
  const [addresses, provinces] = await Promise.all([
    // Sổ địa chỉ lỗi thì vẫn cho nhập địa chỉ mới
    authFetch<SavedAddress[]>("/api/customers/me/addresses", { cache: "no-store" }).catch(() => [] as SavedAddress[]),
    getProvinces().catch(() => []),
  ]);
  return <CheckoutForm email={session.user.email ?? ""} addresses={addresses} provinces={provinces} />;
}
