"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { ErrorNote } from "@/components/admin/admin-ui";
import { cancelOrderAdmin, changeOrderStatus, shipOrder, type AdminError } from "@/lib/admin-actions";
import { CARRIERS, orderActions } from "@/lib/admin-shared";
import type { OrderStatus } from "@/lib/checkout-shared";

/** Thao tác trên một đơn theo trạng thái hiện tại; giao hàng chọn hãng, hủy bắt buộc lý do. */
export function OrderActions({ code, status, defaultCarrier }: { code: string; status: OrderStatus; defaultCarrier: string }) {
  const router = useRouter();
  const actions = orderActions(status);
  const [panel, setPanel] = useState<"ship" | "cancel" | null>(null);
  const [carrier, setCarrier] = useState(defaultCarrier);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<AdminError | null>(null);
  const [pending, start] = useTransition();

  if (actions.length === 0) return <p className="m-0 text-[13px] text-muted">Đơn ở trạng thái cuối, không còn thao tác.</p>;

  const run = (fn: () => Promise<{ ok: true } | AdminError>) =>
    start(async () => {
      setError(null);
      const res = await fn();
      if (!res.ok) {
        setError(res);
        return;
      }
      setPanel(null);
      setReason("");
      router.refresh();
    });

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap gap-2">
        {actions.map((a) => (
          <button
            key={a.label}
            type="button"
            disabled={pending}
            onClick={() => {
              if (a.kind === "status") run(() => changeOrderStatus(code, a.to));
              else setPanel(a.kind);
            }}
            className={`btn-admin ${"primary" in a && a.primary ? "" : "btn-admin-light"} ${a.kind === "cancel" ? "text-sale" : ""}`}
          >
            {a.label}
          </button>
        ))}
      </div>

      {panel === "ship" ? (
        <div className="flex flex-wrap items-end gap-2 rounded-lg bg-admin-bg p-3">
          <label className="flex flex-col gap-1 text-[12px] text-muted">
            Hãng vận chuyển
            <select value={carrier} onChange={(e) => setCarrier(e.target.value)} className="input-admin">
              {CARRIERS.map((c) => (
                <option key={c.code} value={c.code}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <button type="button" disabled={pending} onClick={() => run(() => shipOrder(code, carrier))} className="btn-admin">
            {pending ? "Đang tạo vận đơn…" : "Tạo vận đơn"}
          </button>
          <button type="button" onClick={() => setPanel(null)} className="btn-admin btn-admin-light">
            Thôi
          </button>
        </div>
      ) : null}

      {panel === "cancel" ? (
        <div className="flex flex-wrap items-end gap-2 rounded-lg bg-admin-bg p-3">
          <label className="flex min-w-[240px] flex-1 flex-col gap-1 text-[12px] text-muted">
            Lý do hủy (gửi kèm email cho khách)
            <input value={reason} maxLength={255} onChange={(e) => setReason(e.target.value)} className="input-admin" />
          </label>
          <button type="button" disabled={pending || !reason.trim()} onClick={() => run(() => cancelOrderAdmin(code, reason))} className="btn-admin">
            {pending ? "Đang hủy…" : "Xác nhận hủy"}
          </button>
          <button type="button" onClick={() => setPanel(null)} className="btn-admin btn-admin-light">
            Giữ đơn
          </button>
        </div>
      ) : null}

      {error ? <ErrorNote message={error.message} traceId={error.traceId} /> : null}
    </div>
  );
}
