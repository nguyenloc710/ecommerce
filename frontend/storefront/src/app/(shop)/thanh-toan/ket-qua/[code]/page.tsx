import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Suspense } from "react";
import { AutoRefresh } from "@/components/order/auto-refresh";
import { cancelReasonLabel } from "@/lib/checkout-shared";
import { getMyOrder } from "@/lib/orders";
import { requireSession } from "@/lib/session";

export const metadata: Metadata = { title: "Kết quả thanh toán", robots: { index: false } };

/** Chờ IPN: làm mới mỗi 3 giây, tối đa 1 phút rồi để khách tự bấm kiểm tra lại. */
const REFRESH_MS = 3_000;
const REFRESH_TRIES = 20;

/**
 * Return URL của cổng thanh toán (VNPay). Kết quả chính thức đến qua IPN ở order-service, nên trang này chỉ đọc trạng
 * thái đơn: đã xác nhận → trang cảm ơn; còn chờ → tự làm mới; đã hủy → báo thất bại. Tham số cổng gửi kèm (vnp_*) chỉ để
 * gợi ý, không dùng để quyết định.
 */
export default function Page({ params, searchParams }: PageProps<"/thanh-toan/ket-qua/[code]">) {
  return (
    <main className="pad flex flex-1 items-center justify-center py-16 lg:py-24">
      <div className="flex w-full max-w-[460px] flex-col items-center gap-5 text-center">
        <Suspense fallback={<div className="h-40 w-full animate-pulse bg-stone" />}>
          <PaymentResult params={params} searchParams={searchParams} />
        </Suspense>
      </div>
    </main>
  );
}

async function PaymentResult({ params, searchParams }: Pick<PageProps<"/thanh-toan/ket-qua/[code]">, "params" | "searchParams">) {
  const { code } = await params;
  const sp = await searchParams;
  await requireSession(`/thanh-toan/ket-qua/${code}`);
  const order = await getMyOrder(code);
  const detail = `/tai-khoan/don-hang/${encodeURIComponent(order.code)}`;

  if (order.status !== "AWAITING_PAYMENT" && order.status !== "CANCELLED" && order.paidAt) {
    redirect(`/thanh-toan/thanh-cong/${encodeURIComponent(order.code)}`);
  }

  if (order.status === "CANCELLED") {
    return (
      <>
        <span className="eyebrow text-sale">Thanh toán không thành công</span>
        <h1 className="font-display m-0 text-[34px] font-medium leading-tight">Đơn {order.code} đã hủy</h1>
        <p className="m-0 text-[14px] text-muted">
          {order.cancelReason ? `${cancelReasonLabel(order.cancelReason)}. ` : ""}Bạn chưa bị trừ tiền; nếu có, khoản tiền sẽ được hoàn theo phương thức ban
          đầu.
        </p>
        <div className="flex gap-4">
          <Link href="/gio-hang" className="btn">
            Đặt lại
          </Link>
          <Link href={detail} className="btn btn-light">
            Xem đơn
          </Link>
        </div>
      </>
    );
  }

  // vnp_ResponseCode khác 00: khách hủy hoặc ngân hàng từ chối; đơn sẽ chuyển Hủy khi IPN tới hoặc hết hạn thanh toán
  const gatewayDeclined = typeof sp.vnp_ResponseCode === "string" && sp.vnp_ResponseCode !== "00";
  return (
    <>
      <span className="eyebrow text-muted">Đơn hàng {order.code}</span>
      <h1 className="font-display m-0 text-[34px] font-medium leading-tight">
        {gatewayDeclined ? "Giao dịch chưa thành công" : "Đang xác nhận thanh toán"}
      </h1>
      <p className="m-0 text-[14px] text-muted">
        {gatewayDeclined
          ? "Cổng thanh toán báo giao dịch không hoàn tất. Đơn chưa được thanh toán và sẽ tự hủy nếu không thanh toán trong 15 phút."
          : "Chúng tôi đang chờ cổng thanh toán xác nhận, thường mất vài giây. Trang sẽ tự cập nhật."}
      </p>
      {gatewayDeclined ? null : <AutoRefresh intervalMs={REFRESH_MS} maxTries={REFRESH_TRIES} />}
      <Link href={detail} className="eyebrow ul">
        Xem chi tiết đơn
      </Link>
    </>
  );
}
