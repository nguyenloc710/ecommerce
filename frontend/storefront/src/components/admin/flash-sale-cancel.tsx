"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { ErrorNote } from "@/components/admin/admin-ui";
import { cancelFlashSale, type AdminError } from "@/lib/admin-actions";

/** Dừng chương trình (xác nhận hai bước, không dùng confirm() của trình duyệt). */
export function FlashSaleCancel({ id, version }: { id: string; version: number }) {
  const router = useRouter();
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<AdminError | null>(null);
  const [pending, start] = useTransition();

  const cancel = () =>
    start(async () => {
      setError(null);
      const res = await cancelFlashSale(id, version);
      if (!res.ok) setError(res);
      else router.refresh();
      setConfirming(false);
    });

  return (
    <div className="flex flex-col items-end gap-2">
      {confirming ? (
        <div className="flex items-center gap-2 text-[13px]">
          <span className="text-muted">Dừng ngay? Đơn đã giữ suất vẫn giữ giá.</span>
          <button type="button" onClick={cancel} disabled={pending} className="btn-admin">
            {pending ? "Đang dừng…" : "Dừng chương trình"}
          </button>
          <button type="button" onClick={() => setConfirming(false)} className="btn-admin btn-admin-light">
            Thôi
          </button>
        </div>
      ) : (
        <button type="button" onClick={() => setConfirming(true)} className="btn-admin btn-admin-light">
          Dừng chương trình
        </button>
      )}
      {error ? <ErrorNote message={error.message} traceId={error.traceId} /> : null}
    </div>
  );
}
