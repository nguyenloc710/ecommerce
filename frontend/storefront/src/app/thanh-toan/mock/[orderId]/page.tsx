import type { Metadata } from "next";
import { notFound, redirect } from "next/navigation";
import { Suspense } from "react";
import { MockPayButtons } from "@/components/order/mock-pay-buttons";
import { formatVnd } from "@/lib/format";
import { getMyOrder } from "@/lib/orders";
import { requireSession } from "@/lib/session";

export const metadata: Metadata = { title: "Cổng thanh toán thử nghiệm", robots: { index: false } };

/**
 * Cổng thanh toán giả (B-18). order-service tạo payUrl `/thanh-toan/mock/{orderId}`; storefront thêm `?ma=` (mã đơn)
 * để tra đơn bằng API của khách rồi đối chiếu orderId, không cần thêm endpoint tra theo id.
 */
export default function Page({ params, searchParams }: PageProps<"/thanh-toan/mock/[orderId]">) {
  return (
    <main className="pad flex flex-1 items-center justify-center py-16 lg:py-24">
      <div className="w-full max-w-[420px] border border-line p-8">
        <Suspense fallback={<div className="h-48 animate-pulse bg-stone" />}>
          <MockGateway params={params} searchParams={searchParams} />
        </Suspense>
      </div>
    </main>
  );
}

async function MockGateway({ params, searchParams }: Pick<PageProps<"/thanh-toan/mock/[orderId]">, "params" | "searchParams">) {
  const { orderId } = await params;
  const { ma } = await searchParams;
  if (typeof ma !== "string") notFound();
  await requireSession(`/thanh-toan/mock/${orderId}?ma=${encodeURIComponent(ma)}`);
  const order = await getMyOrder(ma);
  if (order.id !== orderId) notFound();
  if (order.status !== "AWAITING_PAYMENT") redirect(`/tai-khoan/don-hang/${encodeURIComponent(order.code)}`);

  return (
    <div className="flex flex-col gap-6 text-center">
      <span className="eyebrow text-muted">Cổng thanh toán thử nghiệm</span>
      <div className="flex flex-col gap-1">
        <span className="text-[13px] text-muted">Đơn hàng {order.code}</span>
        <span className="font-display text-[40px] leading-none">{formatVnd(order.total)}</span>
      </div>
      <p className="m-0 text-[12px] text-muted">Trang này thay cho VNPay/MoMo khi phát triển. Đơn chưa thanh toán sẽ tự hủy sau 15 phút.</p>
      <MockPayButtons orderCode={order.code} />
    </div>
  );
}
