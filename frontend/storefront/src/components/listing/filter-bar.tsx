"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import type { FacetsView } from "@/lib/catalog-shared";
import { optionLabel } from "@/lib/catalog-shared";
import { formatVnd } from "@/lib/format";
import { PAGE_SIZE, SORT_LABEL, SORT_VALUES, activeFilterCount, buildPageQuery, type ListingParams, type SortValue } from "@/lib/listing-params";

type Props = { facets: FacetsView; params: ListingParams; basePath: string; total: number };

/** Thanh lọc dính trên đầu (UI-08): Bộ lọc +, sắp xếp, mật độ lưới; panel facet và chip đang lọc. Trạng thái nằm trên URL. */
export function FilterBar({ facets, params, basePath, total }: Props) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [pending, startTransition] = useTransition();
  const [priceMin, setPriceMin] = useState(params.priceMin?.toString() ?? "");
  const [priceMax, setPriceMax] = useState(params.priceMax?.toString() ?? "");
  const active = activeFilterCount(params);

  const navigate = (overrides: Partial<ListingParams>) => {
    startTransition(() => router.replace(`${basePath}${buildPageQuery(params, overrides)}`, { scroll: false }));
  };

  const toggleOption = (name: string, value: string) => {
    const current = params.options[name] ?? [];
    const next = current.includes(value) ? current.filter((v) => v !== value) : [...current, value];
    const options = { ...params.options, [name]: next };
    if (next.length === 0) delete options[name];
    navigate({ options, size: PAGE_SIZE });
  };

  const applyPrice = () => {
    const min = priceMin ? Number(priceMin.replace(/[^\d]/g, "")) : undefined;
    const max = priceMax ? Number(priceMax.replace(/[^\d]/g, "")) : undefined;
    navigate({ priceMin: min, priceMax: max });
  };

  const clearAll = () => {
    setPriceMin("");
    setPriceMax("");
    navigate({ options: {}, priceMin: undefined, priceMax: undefined });
  };

  return (
    <div className={`sticky top-14 z-20 bg-paper lg:top-20 ${pending ? "opacity-70" : ""}`}>
      <div className="pad flex h-12 items-center justify-between gap-4 border-y border-line lg:h-14">
        <div className="flex items-center gap-5 lg:gap-7">
          <button type="button" aria-expanded={open} onClick={() => setOpen((o) => !o)} className="inline-flex h-11 items-center gap-2 text-[11px] font-medium uppercase tracking-[0.12em] lg:text-[12px]">
            Bộ lọc
            <span aria-hidden="true" className="text-base font-light leading-none">{open ? "–" : "+"}</span>
            {active > 0 ? <span className="inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-ink px-1 text-[10px] text-paper">{active}</span> : null}
          </button>
          <span className="hidden text-[12px] text-muted lg:inline">{total} sản phẩm</span>
        </div>
        <div className="flex items-center gap-3 lg:gap-6">
          <label className="flex items-center gap-2">
            <span className="eyebrow hidden text-muted lg:inline">Sắp xếp</span>
            <select
              value={params.sort}
              onChange={(e) => navigate({ sort: e.target.value as SortValue })}
              className="h-11 cursor-pointer bg-transparent text-[11px] font-medium uppercase tracking-[0.12em] outline-none lg:text-[12px]"
            >
              {SORT_VALUES.map((s) => (
                <option key={s} value={s}>
                  {SORT_LABEL[s]}
                </option>
              ))}
            </select>
          </label>
          <div role="group" aria-label="Mật độ lưới" className="flex border-l border-line pl-3 lg:pl-4">
            <button type="button" aria-label="Lưới thưa" aria-pressed={params.view === "wide"} onClick={() => navigate({ view: "wide" })} className={`inline-flex h-11 w-9 items-center justify-center ${params.view === "wide" ? "text-ink" : "text-[#bbb]"}`}>
              <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="18" /><rect x="13" y="3" width="8" height="18" /></svg>
            </button>
            <button type="button" aria-label="Lưới dày" aria-pressed={params.view === "compact"} onClick={() => navigate({ view: "compact" })} className={`inline-flex h-11 w-9 items-center justify-center ${params.view === "compact" ? "text-ink" : "text-[#bbb]"}`}>
              <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><rect x="2" y="3" width="4" height="18" /><rect x="7.5" y="3" width="4" height="18" /><rect x="13" y="3" width="4" height="18" /><rect x="18.5" y="3" width="4" height="18" /></svg>
            </button>
          </div>
        </div>
      </div>

      {open ? (
        <div className="pad grid grid-cols-2 gap-6 border-b border-line py-5 lg:grid-cols-5 lg:gap-8 lg:py-7">
          {facets.options.map((facet) => (
            <fieldset key={facet.name} className="min-w-0">
              <legend className="eyebrow mb-3">{optionLabel(facet.name)}</legend>
              <div className="flex flex-wrap gap-1.5">
                {facet.values.map((v) => {
                  const selected = params.options[facet.name]?.includes(v.value) ?? false;
                  return (
                    <button
                      key={v.value}
                      type="button"
                      aria-pressed={selected}
                      onClick={() => toggleOption(facet.name, v.value)}
                      className={`inline-flex h-9 items-center gap-1.5 border px-3 text-[12px] ${selected ? "border-ink bg-ink text-paper" : "border-line bg-paper hover:border-ink"}`}
                    >
                      {v.value}
                      <span className={`text-[10px] ${selected ? "text-paper/70" : "text-muted"}`}>{v.count}</span>
                    </button>
                  );
                })}
              </div>
            </fieldset>
          ))}
          <fieldset className="min-w-0">
            <legend className="eyebrow mb-3">Giá</legend>
            <div className="flex items-center gap-2">
              <label className="sr-only" htmlFor="price-min">Giá từ</label>
              <input id="price-min" inputMode="numeric" placeholder={facets.priceMin ? formatVnd(facets.priceMin) : "Từ"} value={priceMin} onChange={(e) => setPriceMin(e.target.value)} className="h-10 w-full min-w-0 border border-line px-2.5 text-[13px] outline-none focus:border-ink" />
              <span className="text-muted">–</span>
              <label className="sr-only" htmlFor="price-max">Giá đến</label>
              <input id="price-max" inputMode="numeric" placeholder={facets.priceMax ? formatVnd(facets.priceMax) : "Đến"} value={priceMax} onChange={(e) => setPriceMax(e.target.value)} className="h-10 w-full min-w-0 border border-line px-2.5 text-[13px] outline-none focus:border-ink" />
            </div>
            <button type="button" onClick={applyPrice} className="btn btn-light mt-3 min-h-10 px-5 text-[11px]">
              Áp dụng
            </button>
          </fieldset>
        </div>
      ) : null}

      {active > 0 ? (
        <div className="pad flex flex-wrap items-center gap-2 py-3">
          {Object.entries(params.options).flatMap(([name, values]) =>
            values.map((value) => (
              <button key={`${name}:${value}`} type="button" onClick={() => toggleOption(name, value)} className="inline-flex h-8 items-center gap-2 border border-ink px-3 text-[12px]">
                {optionLabel(name)}: {value}
                <span aria-hidden="true">×</span>
              </button>
            )),
          )}
          {params.priceMin !== undefined || params.priceMax !== undefined ? (
            <button type="button" onClick={() => { setPriceMin(""); setPriceMax(""); navigate({ priceMin: undefined, priceMax: undefined }); }} className="inline-flex h-8 items-center gap-2 border border-ink px-3 text-[12px]">
              {params.priceMin !== undefined ? formatVnd(params.priceMin) : "0₫"} – {params.priceMax !== undefined ? formatVnd(params.priceMax) : "∞"}
              <span aria-hidden="true">×</span>
            </button>
          ) : null}
          <button type="button" onClick={clearAll} className="eyebrow ul ml-2 text-[11px] text-muted">
            Xóa tất cả
          </button>
        </div>
      ) : null}
    </div>
  );
}
