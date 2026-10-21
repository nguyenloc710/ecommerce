"use client";

import { useState, useTransition } from "react";
import { Badge } from "@/components/admin/admin-ui";
import { updateStock } from "@/lib/admin-actions";
import type { InventoryRow } from "@/lib/admin-shared";

/**
 * Một dòng tồn kho sửa tại chỗ: đặt số thực có (onHand, giá trị tuyệt đối) và ngưỡng báo sắp hết.
 * Giữ dòng mới nhất do Server Action trả về (kể cả version) thay vì props ban đầu.
 */
export function StockRow({ row: initial }: { row: InventoryRow }) {
  const [row, setRow] = useState(initial);
  const [onHand, setOnHand] = useState(String(initial.onHand));
  const [threshold, setThreshold] = useState(String(initial.lowStockThreshold));
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null);
  const [pending, start] = useTransition();
  const dirty = onHand !== String(row.onHand) || threshold !== String(row.lowStockThreshold);

  const save = () =>
    start(async () => {
      const res = await updateStock(row.skuId, Number(onHand), Number(threshold), row.version);
      if (!res.ok) {
        setMessage({ ok: false, text: res.traceId ? `${res.message} (mã lỗi ${res.traceId})` : res.message });
        return;
      }
      setRow(res.row);
      setOnHand(String(res.row.onHand));
      setThreshold(String(res.row.lowStockThreshold));
      setMessage({ ok: true, text: "Đã lưu" });
    });

  return (
    <tr className="border-t border-admin-line align-top">
      <td className="px-4 py-3">
        <div className="font-medium">{row.productName}</div>
        <div className="text-[12px] text-muted">
          {row.variantLabel} · {row.skuCode}
        </div>
      </td>
      <td className="px-4 py-3">
        <input
          value={onHand}
          onChange={(e) => {
            setOnHand(e.target.value);
            setMessage(null);
          }}
          inputMode="numeric"
          aria-label={`Số thực có của ${row.skuCode}`}
          className="input-admin w-[90px]"
        />
      </td>
      <td className="px-4 py-3 text-muted">{row.reserved}</td>
      <td className="px-4 py-3">
        <span className="mr-2">{row.available}</span>
        {row.available <= 0 ? <Badge tone="critical">Hết</Badge> : row.lowStock ? <Badge tone="warning">Sắp hết</Badge> : null}
      </td>
      <td className="px-4 py-3">
        <input
          value={threshold}
          onChange={(e) => {
            setThreshold(e.target.value);
            setMessage(null);
          }}
          inputMode="numeric"
          aria-label={`Ngưỡng sắp hết của ${row.skuCode}`}
          className="input-admin w-[70px]"
        />
      </td>
      <td className="px-4 py-3">
        <button type="button" disabled={!dirty || pending} onClick={save} className="btn-admin">
          {pending ? "Đang lưu…" : "Lưu"}
        </button>
        {message ? <div className={`mt-1 max-w-[220px] text-[12px] ${message.ok ? "text-ok" : "text-sale"}`}>{message.text}</div> : null}
      </td>
    </tr>
  );
}
