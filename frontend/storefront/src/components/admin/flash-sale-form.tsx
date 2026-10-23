"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { ErrorNote } from "@/components/admin/admin-ui";
import { createFlashSale, searchSkus, type AdminError } from "@/lib/admin-actions";
import type { InventoryRow } from "@/lib/admin-shared";
import { FLASH_SALE_MAX_ITEMS, flashSaleSchema, type FlashSaleFormValues } from "@/lib/flash-sale-shared";

/** Tạo chương trình flash sale: khung giờ theo giờ trình duyệt, chọn SKU bằng ô tìm kiếm, mỗi SKU một giá + số suất. */
export function FlashSaleForm() {
  const router = useRouter();
  const [error, setError] = useState<AdminError | null>(null);
  const [pending, start] = useTransition();
  const { register, control, handleSubmit, formState } = useForm<FlashSaleFormValues>({
    resolver: zodResolver(flashSaleSchema),
    defaultValues: { name: "", startsAt: "", endsAt: "", items: [] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "items" });
  const errors = formState.errors;

  const onSubmit = (values: FlashSaleFormValues) =>
    start(async () => {
      setError(null);
      // datetime-local không có múi giờ: đổi ở trình duyệt để server nhận đúng thời điểm
      const res = await createFlashSale({ ...values, startsAt: new Date(values.startsAt).toISOString(), endsAt: new Date(values.endsAt).toISOString() });
      if (!res.ok) {
        setError(res);
        return;
      }
      router.push(`/admin/flash-sale/${res.id}`);
    });

  const addSku = (row: InventoryRow) => {
    if (fields.some((f) => f.skuId === row.skuId) || fields.length >= FLASH_SALE_MAX_ITEMS) return;
    append({ skuId: row.skuId, title: `${row.productName} · ${row.variantLabel} (${row.skuCode})`, salePrice: "", quota: "", perCustomerLimit: "1" });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <div className="card flex flex-col gap-4 p-4">
        <label className="flex flex-col gap-1.5 text-[13px]">
          <span className="font-medium">Tên chương trình</span>
          <input {...register("name")} className="input-admin" placeholder="Flash sale 11.11" />
          {errors.name ? <span className="text-[12px] text-[#8e1f0b]">{errors.name.message}</span> : null}
        </label>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="flex flex-col gap-1.5 text-[13px]">
            <span className="font-medium">Bắt đầu</span>
            <input type="datetime-local" {...register("startsAt")} className="input-admin" />
            {errors.startsAt ? <span className="text-[12px] text-[#8e1f0b]">{errors.startsAt.message}</span> : null}
          </label>
          <label className="flex flex-col gap-1.5 text-[13px]">
            <span className="font-medium">Kết thúc</span>
            <input type="datetime-local" {...register("endsAt")} className="input-admin" />
            {errors.endsAt ? <span className="text-[12px] text-[#8e1f0b]">{errors.endsAt.message}</span> : null}
          </label>
        </div>
      </div>

      <div className="card flex flex-col gap-3 p-4">
        <h2 className="m-0 text-[14px] font-semibold">Sản phẩm ({fields.length})</h2>
        <SkuPicker onPick={addSku} />
        {errors.items?.message ? <span className="text-[12px] text-[#8e1f0b]">{errors.items.message}</span> : null}
        {fields.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[680px] border-collapse text-[13px]">
              <thead>
                <tr className="bg-admin-bg text-left text-[12px] text-muted">
                  <th className="px-3 py-2 font-medium">SKU</th>
                  <th className="px-3 py-2 font-medium">Giá flash (đ)</th>
                  <th className="px-3 py-2 font-medium">Số suất</th>
                  <th className="px-3 py-2 font-medium">Tối đa mỗi khách</th>
                  <th className="px-3 py-2" />
                </tr>
              </thead>
              <tbody>
                {fields.map((field, index) => {
                  const e = errors.items?.[index];
                  return (
                    <tr key={field.id} className="border-t border-admin-line align-top">
                      <td className="px-3 py-2">{field.title}</td>
                      <td className="px-3 py-2">
                        <input inputMode="numeric" aria-label="Giá flash" {...register(`items.${index}.salePrice`)} className="input-admin w-32" />
                        {e?.salePrice ? <span className="block text-[12px] text-[#8e1f0b]">{e.salePrice.message}</span> : null}
                      </td>
                      <td className="px-3 py-2">
                        <input inputMode="numeric" aria-label="Số suất" {...register(`items.${index}.quota`)} className="input-admin w-24" />
                        {e?.quota ? <span className="block text-[12px] text-[#8e1f0b]">{e.quota.message}</span> : null}
                      </td>
                      <td className="px-3 py-2">
                        <input inputMode="numeric" aria-label="Tối đa mỗi khách" {...register(`items.${index}.perCustomerLimit`)} className="input-admin w-20" />
                        {e?.perCustomerLimit ? <span className="block text-[12px] text-[#8e1f0b]">{e.perCustomerLimit.message}</span> : null}
                      </td>
                      <td className="px-3 py-2 text-right">
                        <button type="button" onClick={() => remove(index)} className="btn-admin btn-admin-light">
                          Bỏ
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="m-0 text-[12px] text-muted">Giá flash phải thấp hơn giá niêm yết. Tối đa mỗi khách = 0 nghĩa là không giới hạn.</p>
      </div>

      {error ? <ErrorNote message={error.message} traceId={error.traceId} /> : null}
      <div>
        <button type="submit" disabled={pending} className="btn-admin">
          {pending ? "Đang tạo…" : "Tạo chương trình"}
        </button>
      </div>
    </form>
  );
}

function SkuPicker({ onPick }: { onPick: (row: InventoryRow) => void }) {
  const [q, setQ] = useState("");
  const [results, setResults] = useState<InventoryRow[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [pending, start] = useTransition();

  const search = () =>
    start(async () => {
      setMessage(null);
      const res = await searchSkus(q);
      if (!res.ok) {
        setMessage(res.message);
        return;
      }
      setResults(res.items);
      if (res.items.length === 0) setMessage("Không tìm thấy SKU nào.");
    });

  return (
    <div className="flex flex-col gap-2">
      <div className="flex gap-2">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              search();
            }
          }}
          placeholder="Tìm theo mã SKU hoặc tên sản phẩm"
          aria-label="Tìm SKU"
          className="input-admin max-w-[320px]"
        />
        <button type="button" onClick={search} disabled={pending} className="btn-admin btn-admin-light">
          {pending ? "Đang tìm…" : "Tìm"}
        </button>
      </div>
      {message ? <span className="text-[12px] text-muted">{message}</span> : null}
      {results.length > 0 ? (
        <ul className="m-0 flex list-none flex-col rounded-lg border border-admin-line p-0">
          {results.map((r) => (
            <li key={r.skuId} className="flex items-center justify-between gap-3 border-t border-admin-line px-3 py-2 text-[13px] first:border-t-0">
              <span>
                {r.productName} · {r.variantLabel} <span className="text-muted">({r.skuCode}, còn {r.available})</span>
              </span>
              <button type="button" onClick={() => onPick(r)} className="btn-admin btn-admin-light">
                Thêm
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
