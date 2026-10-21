"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { useForm } from "react-hook-form";
import { ErrorNote } from "@/components/admin/admin-ui";
import { deleteVariant, saveVariant, type AdminError } from "@/lib/admin-actions";
import { formatOptions, variantSchema, type VariantAdmin, type VariantFormValues } from "@/lib/admin-shared";

/** Bảng biến thể (SKU): mỗi dòng là một form nhỏ lưu riêng vì mỗi biến thể có version riêng. */
export function VariantsEditor({ productId, variants }: { productId: string; variants: VariantAdmin[] }) {
  const [adding, setAdding] = useState(variants.length === 0);
  return (
    <section className="card flex flex-col gap-3 p-4">
      <div className="flex items-center justify-between">
        <h2 className="m-0 text-[14px] font-semibold">Biến thể</h2>
        {!adding ? (
          <button type="button" onClick={() => setAdding(true)} className="btn-admin btn-admin-light">
            Thêm biến thể
          </button>
        ) : null}
      </div>
      <p className="m-0 text-[12px] text-muted">
        Thuộc tính nhập dạng <code>color=Bạc, size=6</code>. Tồn kho chỉnh ở trang Tồn kho sau khi tạo biến thể.
      </p>
      <div className="hidden grid-cols-[1.2fr_1.6fr_1fr_1fr_0.9fr_auto] gap-2 px-1 text-[12px] text-muted lg:grid">
        <span>SKU</span>
        <span>Thuộc tính</span>
        <span>Giá bán</span>
        <span>Giá gốc</span>
        <span>Trạng thái</span>
        <span />
      </div>
      {variants.map((v) => (
        <VariantRow key={`${v.id}:${v.version}`} productId={productId} variant={v} />
      ))}
      {adding ? <VariantRow productId={productId} variant={null} onDone={() => setAdding(false)} /> : null}
    </section>
  );
}

function VariantRow({ productId, variant, onDone }: { productId: string; variant: VariantAdmin | null; onDone?: () => void }) {
  const router = useRouter();
  const [error, setError] = useState<AdminError | null>(null);
  const [pending, start] = useTransition();
  const { register, handleSubmit, formState, reset } = useForm<VariantFormValues>({
    resolver: zodResolver(variantSchema),
    defaultValues: {
      skuCode: variant?.skuCode ?? "",
      options: variant ? formatOptions(variant.optionValues) : "",
      price: variant ? String(variant.price) : "",
      compareAtPrice: variant?.compareAtPrice != null ? String(variant.compareAtPrice) : "",
      status: variant?.status ?? "ACTIVE",
    },
  });
  const fieldError = Object.values(formState.errors)[0]?.message;

  const onSubmit = (values: VariantFormValues) =>
    start(async () => {
      setError(null);
      const res = await saveVariant(productId, variant?.id ?? null, values, variant?.version);
      if (!res.ok) {
        setError(res);
        return;
      }
      if (!variant) {
        reset();
        onDone?.();
      }
      router.refresh();
    });

  const remove = () =>
    start(async () => {
      const res = await deleteVariant(productId, variant!.id);
      if (!res.ok) setError(res);
      else router.refresh();
    });

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-1 border-t border-admin-line pt-3">
      <div className="grid grid-cols-2 gap-2 lg:grid-cols-[1.2fr_1.6fr_1fr_1fr_0.9fr_auto]">
        <input {...register("skuCode")} placeholder="SKU" aria-label="SKU" className="input-admin" />
        <input {...register("options")} placeholder="color=Bạc, size=6" aria-label="Thuộc tính" className="input-admin" />
        <input {...register("price")} inputMode="numeric" placeholder="Giá bán" aria-label="Giá bán" className="input-admin" />
        <input {...register("compareAtPrice")} inputMode="numeric" placeholder="Giá gốc" aria-label="Giá gốc" className="input-admin" />
        <select {...register("status")} aria-label="Trạng thái" className="input-admin">
          <option value="ACTIVE">Đang bán</option>
          <option value="HIDDEN">Ẩn</option>
        </select>
        <div className="flex gap-2">
          <button type="submit" disabled={pending} className="btn-admin">
            {variant ? "Lưu" : "Thêm"}
          </button>
          {variant ? (
            <button type="button" disabled={pending} onClick={remove} aria-label="Xóa biến thể" className="btn-admin btn-admin-light text-sale">
              Xóa
            </button>
          ) : (
            <button type="button" onClick={onDone} className="btn-admin btn-admin-light">
              Thôi
            </button>
          )}
        </div>
      </div>
      {fieldError ? <span className="text-[12px] text-sale">{fieldError}</span> : null}
      {error ? <ErrorNote message={error.message} traceId={error.traceId} /> : null}
    </form>
  );
}
