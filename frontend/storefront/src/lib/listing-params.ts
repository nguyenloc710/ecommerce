/**
 * Tham số trang danh sách giữ trên URL (UI-08): lọc, sắp xếp, số lượng hiển thị, mật độ lưới.
 * Dùng chung cho Server Component (đọc searchParams) và Client Component (sửa URL).
 */
export type SearchParamsLike = Record<string, string | string[] | undefined>;

export const SORT_VALUES = ["newest", "price_asc", "price_desc"] as const;
export type SortValue = (typeof SORT_VALUES)[number];
export const SORT_LABEL: Record<SortValue, string> = {
  newest: "Mới nhất",
  price_asc: "Giá tăng dần",
  price_desc: "Giá giảm dần",
};

export const PAGE_SIZE = 24;
export const MAX_PAGE_SIZE = 96;
export const OPTION_PREFIX = "opt.";

export type ListingParams = {
  category?: string;
  brand?: string;
  q?: string;
  options: Record<string, string[]>;
  priceMin?: number;
  priceMax?: number;
  sort: SortValue;
  size: number;
  /** Mật độ lưới: desktop 2|4 cột, mobile 1|2 */
  view: "compact" | "wide";
};

function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function toNumber(value: string | undefined): number | undefined {
  if (!value) return undefined;
  const n = Number(value.replace(/[^\d]/g, ""));
  return Number.isFinite(n) && n >= 0 ? n : undefined;
}

/** Đọc URL → ListingParams. `fixed` là phần cố định của trang (danh mục từ path, từ khóa...). */
export function parseListingParams(sp: SearchParamsLike, fixed: Partial<Pick<ListingParams, "category" | "brand" | "q">> = {}): ListingParams {
  const options: Record<string, string[]> = {};
  for (const [key, raw] of Object.entries(sp)) {
    if (!key.startsWith(OPTION_PREFIX) || raw === undefined) continue;
    const values = (Array.isArray(raw) ? raw : [raw]).flatMap((v) => v.split(",")).map((v) => v.trim()).filter(Boolean);
    if (values.length) options[key.slice(OPTION_PREFIX.length)] = [...new Set(values)];
  }
  const sortRaw = first(sp.sort);
  const sort = (SORT_VALUES as readonly string[]).includes(sortRaw ?? "") ? (sortRaw as SortValue) : "newest";
  const size = Math.min(MAX_PAGE_SIZE, Math.max(PAGE_SIZE, toNumber(first(sp.size)) ?? PAGE_SIZE));
  return {
    category: fixed.category,
    brand: fixed.brand ?? first(sp.brand),
    q: fixed.q ?? (first(sp.q)?.trim() || undefined),
    options,
    priceMin: toNumber(first(sp.priceMin)),
    priceMax: toNumber(first(sp.priceMax)),
    sort,
    size,
    view: first(sp.view) === "wide" ? "wide" : "compact",
  };
}

/** ListingParams → query string gọi API catalog (bỏ `view`, luôn page=0 vì dùng "Xem thêm"). */
export function buildListingQuery(p: ListingParams): string {
  const qs = new URLSearchParams();
  if (p.category) qs.set("category", p.category);
  if (p.brand) qs.set("brand", p.brand);
  if (p.q) qs.set("q", p.q);
  for (const [name, values] of Object.entries(p.options)) qs.set(`${OPTION_PREFIX}${name}`, values.join(","));
  if (p.priceMin !== undefined) qs.set("priceMin", String(p.priceMin));
  if (p.priceMax !== undefined) qs.set("priceMax", String(p.priceMax));
  qs.set("sort", p.sort);
  qs.set("size", String(p.size));
  qs.set("page", "0");
  return `?${qs.toString()}`;
}

/** Query string cho URL trang (chỉ phần người dùng thay đổi được; phần cố định nằm trong path). */
export function buildPageQuery(p: ListingParams, overrides: Partial<ListingParams> = {}): string {
  const m = { ...p, ...overrides, options: overrides.options ?? p.options };
  const qs = new URLSearchParams();
  if (m.q && !p.category) qs.set("q", m.q);
  for (const [name, values] of Object.entries(m.options)) if (values.length) qs.set(`${OPTION_PREFIX}${name}`, values.join(","));
  if (m.priceMin !== undefined) qs.set("priceMin", String(m.priceMin));
  if (m.priceMax !== undefined) qs.set("priceMax", String(m.priceMax));
  if (m.sort !== "newest") qs.set("sort", m.sort);
  if (m.size !== PAGE_SIZE) qs.set("size", String(m.size));
  if (m.view !== "compact") qs.set("view", m.view);
  const s = qs.toString();
  return s ? `?${s}` : "";
}

export function activeFilterCount(p: ListingParams): number {
  return Object.values(p.options).reduce((n, v) => n + v.length, 0) + (p.priceMin !== undefined || p.priceMax !== undefined ? 1 : 0);
}
