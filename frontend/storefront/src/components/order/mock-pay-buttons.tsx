"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { completeMockPayment } from "@/lib/checkout-actions";

export function MockPayButtons({ orderCode }: { orderCode: string }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();
  const pay = (success: boolean) =>
    start(async () => {
      const res = await completeMockPayment(orderCode, success);
      if (res.ok) router.push(res.next);
      else setError(res.traceId ? `${res.message} (mã lỗi ${res.traceId})` : res.message);
    });
  return (
    <div className="flex flex-col gap-3">
      <button type="button" disabled={pending} onClick={() => pay(true)} className="btn w-full disabled:opacity-60">
        {pending ? "Đang xử lý…" : "Thanh toán thành công"}
      </button>
      <button type="button" disabled={pending} onClick={() => pay(false)} className="btn btn-light w-full disabled:opacity-60">
        Giả lập thất bại
      </button>
      {error ? (
        <p role="alert" className="m-0 text-[13px] text-sale">
          {error}
        </p>
      ) : null}
    </div>
  );
}
