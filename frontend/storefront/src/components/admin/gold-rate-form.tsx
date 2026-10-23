"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { ErrorNote } from "@/components/admin/admin-ui";
import { setGoldRate, type AdminError } from "@/lib/admin-actions";
import { GOLD_KARAT_LABELS, GOLD_KARATS, type GoldKarat } from "@/lib/admin-shared";

/** Nhập giá vàng mới cho một loại vàng; hiệu lực ngay và tính lại giá sản phẩm theo vàng. */
export function GoldRateForm() {
  const router = useRouter();
  const [karat, setKarat] = useState<GoldKarat>("K18");
  const [price, setPrice] = useState("");
  const [note, setNote] = useState("");
  const [error, setError] = useState<AdminError | null>(null);
  const [done, setDone] = useState<string | null>(null);
  const [pending, start] = useTransition();

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    start(async () => {
      setError(null);
      setDone(null);
      const res = await setGoldRate(karat, Number(price.replace(/\./g, "")), note);
      if (!res.ok) {
        setError(res);
        return;
      }
      setDone(`Đã cập nhật ${GOLD_KARAT_LABELS[karat]}, tính lại giá ${res.repricedVariants} biến thể.`);
      setPrice("");
      setNote("");
      router.refresh();
    });
  };

  return (
    <form onSubmit={submit} className="card flex flex-col gap-3 p-4">
      <h2 className="m-0 text-[14px] font-semibold">Nhập giá mới</h2>
      <div className="grid gap-3 sm:grid-cols-[160px_200px_1fr_auto] sm:items-end">
        <label className="flex flex-col gap-1.5 text-[13px]">
          <span className="font-medium">Loại vàng</span>
          <select value={karat} onChange={(e) => setKarat(e.target.value as GoldKarat)} className="input-admin">
            {GOLD_KARATS.map((k) => (
              <option key={k} value={k}>
                {GOLD_KARAT_LABELS[k]}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1.5 text-[13px]">
          <span className="font-medium">Giá bán ra mỗi gam (đ)</span>
          <input value={price} onChange={(e) => setPrice(e.target.value)} inputMode="numeric" placeholder="1.250.000" required className="input-admin" />
        </label>
        <label className="flex flex-col gap-1.5 text-[13px]">
          <span className="font-medium">Ghi chú</span>
          <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={255} placeholder="Theo bảng giá sáng nay" className="input-admin" />
        </label>
        <button type="submit" disabled={pending || !price} className="btn-admin">
          {pending ? "Đang lưu…" : "Cập nhật giá"}
        </button>
      </div>
      <p className="m-0 text-[12px] text-muted">Giá sản phẩm = trọng lượng vàng × giá mỗi gam + phí gia công, làm tròn tới 1.000đ. Đơn đã đặt giữ nguyên giá lúc đặt.</p>
      {done ? <p className="m-0 text-[13px] text-[#0c5132]">{done}</p> : null}
      {error ? <ErrorNote message={error.message} traceId={error.traceId} /> : null}
    </form>
  );
}
