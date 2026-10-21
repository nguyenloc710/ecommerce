"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { cancelMyOrder } from "@/lib/checkout-actions";

/** Hủy đơn khi còn ở trạng thái khách được hủy; bấm lần đầu mở ô lý do (không dùng hộp thoại của trình duyệt). */
export function CancelOrderButton({ orderCode }: { orderCode: string }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();

  if (!open) {
    return (
      <button type="button" onClick={() => setOpen(true)} className="eyebrow ul w-fit cursor-pointer">
        Hủy đơn hàng
      </button>
    );
  }
  const confirm = () =>
    start(async () => {
      const res = await cancelMyOrder(orderCode, reason);
      if (!res.ok) {
        setError(res.traceId ? `${res.message} (mã lỗi ${res.traceId})` : res.message);
        return;
      }
      setOpen(false);
      router.refresh();
    });
  return (
    <div className="flex max-w-[420px] flex-col gap-3 border border-line p-4">
      <label className="flex flex-col gap-1.5 text-[12px] text-muted">
        Lý do hủy (không bắt buộc)
        <input value={reason} maxLength={255} onChange={(e) => setReason(e.target.value)} className="input" />
      </label>
      <div className="flex gap-3">
        <button type="button" disabled={pending} onClick={confirm} className="btn min-h-11 flex-1 disabled:opacity-60">
          {pending ? "Đang hủy…" : "Xác nhận hủy"}
        </button>
        <button type="button" disabled={pending} onClick={() => setOpen(false)} className="btn btn-light min-h-11 flex-1">
          Giữ đơn
        </button>
      </div>
      {error ? (
        <p role="alert" className="m-0 text-[12px] text-sale">
          {error}
        </p>
      ) : null}
    </div>
  );
}
