"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { ErrorNote } from "@/components/admin/admin-ui";
import { saveImages, type AdminError } from "@/lib/admin-actions";
import { formatOptions, type ImageAdmin, type VariantAdmin } from "@/lib/admin-shared";

type Draft = { key: string; id: string | null; variantId: string; url: string; alt: string };

/**
 * Danh sách ảnh theo URL (chưa có upload: module media/R2 làm sau). Lưu là thay toàn bộ danh sách, thứ tự = thứ tự hiển thị.
 * Ảnh trong dữ liệu mẫu dùng đường dẫn tĩnh /img/seed/...; URL ngoài phải nằm trong images.remotePatterns của next.config.
 */
export function ImagesEditor({ productId, images, variants }: { productId: string; images: ImageAdmin[]; variants: VariantAdmin[] }) {
  const router = useRouter();
  const [items, setItems] = useState<Draft[]>(() =>
    [...images].sort((a, b) => a.sortOrder - b.sortOrder).map((i) => ({ key: i.id, id: i.id, variantId: i.variantId ?? "", url: i.url, alt: i.alt ?? "" })),
  );
  const [error, setError] = useState<AdminError | null>(null);
  const [saved, setSaved] = useState(false);
  const [pending, start] = useTransition();

  const update = (key: string, patch: Partial<Draft>) => {
    setSaved(false);
    setItems((list) => list.map((i) => (i.key === key ? { ...i, ...patch } : i)));
  };
  const move = (index: number, delta: number) =>
    setItems((list) => {
      const next = [...list];
      const [item] = next.splice(index, 1);
      next.splice(Math.max(0, Math.min(next.length, index + delta)), 0, item);
      return next;
    });

  const save = () =>
    start(async () => {
      setError(null);
      const res = await saveImages(
        productId,
        items.map((i) => ({ id: i.id ?? "", variantId: i.variantId || null, url: i.url, alt: i.alt })),
      );
      if (!res.ok) {
        setError(res);
        return;
      }
      setSaved(true);
      router.refresh();
    });

  return (
    <section className="card flex flex-col gap-3 p-4">
      <h2 className="m-0 text-[14px] font-semibold">Ảnh</h2>
      {items.map((i, index) => (
        <div key={i.key} className="flex flex-wrap items-center gap-2 border-t border-admin-line pt-3">
          <span className="relative h-14 w-11 flex-none overflow-hidden rounded-md bg-admin-bg">
            {i.url.startsWith("/") ? <Image src={i.url} alt={i.alt} fill sizes="44px" className="object-cover" /> : null}
          </span>
          <input value={i.url} onChange={(e) => update(i.key, { url: e.target.value })} placeholder="URL ảnh" aria-label="URL ảnh" className="input-admin min-w-[200px] flex-[2]" />
          <input value={i.alt} onChange={(e) => update(i.key, { alt: e.target.value })} placeholder="Mô tả ảnh (alt)" aria-label="Mô tả ảnh" className="input-admin min-w-[140px] flex-1" />
          <select value={i.variantId} onChange={(e) => update(i.key, { variantId: e.target.value })} aria-label="Gắn với biến thể" className="input-admin w-auto">
            <option value="">Mọi biến thể</option>
            {variants.map((v) => (
              <option key={v.id} value={v.id}>
                {formatOptions(v.optionValues) || v.skuCode}
              </option>
            ))}
          </select>
          <div className="flex gap-1">
            <button type="button" onClick={() => move(index, -1)} disabled={index === 0} aria-label="Lên" className="btn-admin btn-admin-light px-2">
              ↑
            </button>
            <button type="button" onClick={() => move(index, 1)} disabled={index === items.length - 1} aria-label="Xuống" className="btn-admin btn-admin-light px-2">
              ↓
            </button>
            <button type="button" onClick={() => setItems((list) => list.filter((x) => x.key !== i.key))} aria-label="Bỏ ảnh" className="btn-admin btn-admin-light px-2 text-sale">
              ✕
            </button>
          </div>
        </div>
      ))}
      <div className="flex flex-wrap items-center gap-3 border-t border-admin-line pt-3">
        <button
          type="button"
          onClick={() => setItems((list) => [...list, { key: crypto.randomUUID(), id: null, variantId: "", url: "", alt: "" }])}
          className="btn-admin btn-admin-light"
        >
          Thêm ảnh
        </button>
        <button type="button" disabled={pending} onClick={save} className="btn-admin">
          {pending ? "Đang lưu…" : "Lưu ảnh"}
        </button>
        {saved && !pending ? <span className="text-[13px] text-ok">Đã lưu</span> : null}
      </div>
      {error ? <ErrorNote message={error.message} traceId={error.traceId} /> : null}
    </section>
  );
}
