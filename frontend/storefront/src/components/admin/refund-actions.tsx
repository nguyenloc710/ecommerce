"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { ErrorNote } from "@/components/admin/admin-ui";
import { completeRefund, type AdminError } from "@/lib/admin-actions";

/** Form "Đã hoàn tiền": shop hoàn ở trang quản trị của cổng trước, rồi nhập mã giao dịch hoàn ở đây. */
export function RefundComplete({ refundId, version }: { refundId: string; version: number }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [reference, setReference] = useState("");
  const [note, setNote] = useState("");
  const [error, setError] = useState<AdminError | null>(null);
  const [pending, start] = useTransition();

  const submit = () =>
    start(async () => {
      setError(null);
      const res = await completeRefund(refundId, version, reference, note);
      if (!res.ok) {
        setError(res);
        return;
      }
      setOpen(false);
      router.refresh();
    });

  if (!open) {
    return (
      <div>
        <button type="button" onClick={() => setOpen(true)} className="btn-admin">
          Đánh dấu đã hoàn
        </button>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        <input
          value={reference}
          maxLength={100}
          onChange={(e) => setReference(e.target.value)}
          placeholder="Mã giao dịch hoàn tiền"
          aria-label="Mã giao dịch hoàn tiền"
          className="input-admin min-w-[200px] flex-1"
        />
        <input
          value={note}
          maxLength={255}
          onChange={(e) => setNote(e.target.value)}
          placeholder="Ghi chú (không bắt buộc)"
          aria-label="Ghi chú"
          className="input-admin min-w-[200px] flex-1"
        />
        <button type="button" disabled={pending || !reference.trim()} onClick={submit} className="btn-admin">
          Xác nhận
        </button>
        <button type="button" onClick={() => setOpen(false)} className="btn-admin btn-admin-light">
          Thôi
        </button>
      </div>
      {error ? <ErrorNote message={error.message} traceId={error.traceId} /> : null}
    </div>
  );
}
