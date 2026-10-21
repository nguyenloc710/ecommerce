import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { OrderItems } from "@/components/order/order-items";
import { PAYMENT_LABELS } from "@/lib/checkout-shared";
import { getMyOrder } from "@/lib/orders";
import { requireSession } from "@/lib/session";

export const metadata: Metadata = { title: "Đặt hàng thành công", robots: { index: false } };

export default function Page({ params }: PageProps<"/thanh-toan/thanh-cong/[code]">) {
  return (
    <main className="pad mx-auto flex w-full max-w-[760px] flex-1 flex-col gap-8 py-12 lg:py-20">
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone" />}>
        <Success params={params} />
      </Suspense>
    </main>
  );
}

async function Success({ params }: Pick<PageProps<"/thanh-toan/thanh-cong/[code]">, "params">) {
  const { code } = await params;
  await requireSession(`/thanh-toan/thanh-cong/${code}`);
  const order = await getMyOrder(code);
  const a = order.shippingAddress;
  return (
    <>
      <div className="flex flex-col gap-3 text-center">
        <span className="eyebrow text-muted">Đơn hàng {order.code}</span>
        <h1 className="font-display m-0 text-[36px] font-medium leading-tight lg:text-[48px]">Cảm ơn bạn đã đặt hàng</h1>
        <p className="m-0 text-[14px] text-muted">
          Email xác nhận đã được gửi tới {order.contactEmail}. {order.paymentMethod === "COD" ? "Bạn thanh toán khi nhận hàng." : "Thanh toán đã được ghi nhận."}
        </p>
      </div>
      <div className="grid gap-6 border-y border-line py-6 text-[13px] sm:grid-cols-2">
        <div className="flex flex-col gap-1">
          <span className="eyebrow">Giao tới</span>
          <span>
            {a.recipientName} · {a.phone}
          </span>
          <span className="text-muted">
            {a.line1}, {a.wardName}, {a.provinceName}
          </span>
        </div>
        <div className="flex flex-col gap-1">
          <span className="eyebrow">Thanh toán</span>
          <span>{PAYMENT_LABELS[order.paymentMethod]?.title ?? order.paymentMethod}</span>
        </div>
      </div>
      <OrderItems order={order} />
      <div className="flex flex-wrap justify-center gap-4">
        <Link href={`/tai-khoan/don-hang/${encodeURIComponent(order.code)}`} className="btn">
          Theo dõi đơn hàng
        </Link>
        <Link href="/moi-ve" className="btn btn-light">
          Tiếp tục mua sắm
        </Link>
      </div>
    </>
  );
}
