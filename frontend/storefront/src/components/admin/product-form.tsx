"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { useForm, type FieldError } from "react-hook-form";
import { ErrorNote } from "@/components/admin/admin-ui";
import { deleteProduct, saveProduct, type AdminError } from "@/lib/admin-actions";
import { PRODUCT_STATUS_LABELS, productSchema, type BrandOption, type CategoryOption, type ProductAdmin, type ProductFormValues } from "@/lib/admin-shared";

type Props = { product: ProductAdmin | null; categories: CategoryOption[]; brands: BrandOption[] };

/** Thông tin chung của sản phẩm (ProductUpsertCommand). Biến thể và ảnh sửa ở khối riêng sau khi đã tạo. */
export function ProductForm({ product, categories, brands }: Props) {
  const router = useRouter();
  const [error, setError] = useState<AdminError | null>(null);
  const [saved, setSaved] = useState(false);
  const [pending, start] = useTransition();
  const { register, handleSubmit, formState } = useForm<ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: product?.name ?? "",
      slug: product?.slug ?? "",
      categoryId: product?.categoryId ?? "",
      brandId: product?.brandId ?? "",
      status: product?.status ?? "DRAFT",
      pricingType: product?.pricingType ?? "FIXED",
      description: product?.description ?? "",
      seoTitle: product?.seoTitle ?? "",
      seoDescription: product?.seoDescription ?? "",
    },
  });
  const errors = formState.errors;

  const onSubmit = (values: ProductFormValues) =>
    start(async () => {
      setError(null);
      setSaved(false);
      const res = await saveProduct(product?.id ?? null, values, product?.version);
      if (!res.ok) {
        setError(res);
        return;
      }
      setSaved(true);
      if (!product) router.push(`/admin/san-pham/${res.id}`);
      else router.refresh();
    });

  const [confirmDelete, setConfirmDelete] = useState(false);
  const remove = () =>
    start(async () => {
      const res = await deleteProduct(product!.id);
      if (!res.ok) setError(res);
      else router.push("/admin/san-pham");
    });

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="card flex flex-col gap-4 p-4">
      <h2 className="m-0 text-[14px] font-semibold">Thông tin chung</h2>
      <Field label="Tên sản phẩm" error={errors.name}>
        <input {...register("name")} className="input-admin" />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Slug (để trống để tự sinh từ tên)" error={errors.slug}>
          <input {...register("slug")} className="input-admin" />
        </Field>
        <Field label="Danh mục" error={errors.categoryId}>
          <select {...register("categoryId")} className="input-admin">
            <option value="">Chọn danh mục</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.parentId ? "— " : ""}
                {c.name}
                {c.status === "HIDDEN" ? " (ẩn)" : ""}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Thương hiệu" error={errors.brandId}>
          <select {...register("brandId")} className="input-admin">
            <option value="">Không có</option>
            {brands.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Trạng thái" error={errors.status}>
          <select {...register("status")} className="input-admin">
            {(Object.keys(PRODUCT_STATUS_LABELS) as (keyof typeof PRODUCT_STATUS_LABELS)[]).map((s) => (
              <option key={s} value={s}>
                {PRODUCT_STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Cách tính giá" error={errors.pricingType}>
          <select {...register("pricingType")} className="input-admin">
            <option value="FIXED">Giá cố định</option>
            <option value="GOLD_RATE">Theo giá vàng ngày</option>
          </select>
        </Field>
      </div>
      <Field label="Mô tả" error={errors.description}>
        <textarea rows={5} {...register("description")} className="input-admin py-2" />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Tiêu đề SEO" error={errors.seoTitle}>
          <input {...register("seoTitle")} className="input-admin" />
        </Field>
        <Field label="Mô tả SEO" error={errors.seoDescription}>
          <input {...register("seoDescription")} className="input-admin" />
        </Field>
      </div>
      {error ? <ErrorNote message={error.message} traceId={error.traceId} /> : null}
      <div className="flex flex-wrap items-center gap-3">
        <button type="submit" disabled={pending} className="btn-admin">
          {pending ? "Đang lưu…" : product ? "Lưu thay đổi" : "Tạo sản phẩm"}
        </button>
        {saved && !pending ? <span className="text-[13px] text-ok">Đã lưu</span> : null}
        {product ? (
          confirmDelete ? (
            <span className="ml-auto flex items-center gap-2 text-[13px]">
              Xóa sản phẩm và mọi biến thể?
              <button type="button" disabled={pending} onClick={remove} className="btn-admin bg-sale">
                Xóa
              </button>
              <button type="button" onClick={() => setConfirmDelete(false)} className="btn-admin btn-admin-light">
                Thôi
              </button>
            </span>
          ) : (
            <button type="button" onClick={() => setConfirmDelete(true)} className="btn-admin btn-admin-light ml-auto text-sale">
              Xóa sản phẩm
            </button>
          )
        ) : null}
      </div>
    </form>
  );
}

function Field({ label, error, children }: { label: string; error?: FieldError; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1 text-[12px] text-muted">
      {label}
      {children}
      {error ? <span className="text-sale">{error.message}</span> : null}
    </label>
  );
}
