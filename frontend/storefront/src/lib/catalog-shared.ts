/**
 * Phần dùng được ở cả server lẫn client: kiểu dữ liệu theo hợp đồng catalog-service và các hàm thuần.
 * Hàm gọi API (có "use cache") nằm ở catalog.ts, chỉ import từ Server Component.
 */
import type { ProductCardData } from "@/components/product/product-card";
import type { IconName, Tone } from "@/components/ui/placeholder-image";

export type CategoryTreeNode = {
  id: string;
  name: string;
  slug: string;
  sortOrder: number;
  children: CategoryTreeNode[];
};

export type ProductCardView = {
  id: string;
  slug: string;
  name: string;
  price: number | null;
  compareAtPrice: number | null;
  imageUrl: string | null;
  colors: string[];
  label: string | null;
};

export type PageView<T> = { items: T[]; page: number; size: number; total: number; hasNext: boolean };

export type FacetValue = { value: string; count: number };
export type Facet = { name: string; values: FacetValue[] };
export type FacetsView = { options: Facet[]; attributes: Facet[]; priceMin: number | null; priceMax: number | null };

export type ProductVariantView = {
  id: string;
  skuCode: string;
  optionValues: Record<string, string>;
  price: number;
  compareAtPrice: number | null;
  weightGram: number | null;
};

export type ProductDetail = {
  id: string;
  slug: string;
  name: string;
  description: string | null;
  pricingType: "FIXED" | "GOLD_RATE";
  attributes: Record<string, unknown>;
  brand: { id: string; name: string; slug: string } | null;
  category: { id: string; name: string; slug: string };
  breadcrumb: { id: string; name: string; slug: string }[];
  variants: ProductVariantView[];
  images: { id: string; variantId: string | null; url: string; alt: string | null; sortOrder: number }[];
  options: { name: string; values: string[] }[];
};

export type Availability = { skuId: string; inStock: boolean; lowStock: boolean; remaining: number | null };

/** Tag cache dùng chung cho dữ liệu catalog; admin sửa sản phẩm thì gọi /api/revalidate để làm mới. */
export const CATALOG_CACHE_TAG = "catalog";

// ---- Chuyển đổi sang dữ liệu hiển thị ----

/** Mã nhãn từ API (P-06) → chữ hiển thị. */
const LABEL_TEXT: Record<string, { text: string; tone: ProductCardData["labelTone"] }> = {
  NEW: { text: "Mới", tone: "default" },
  BESTSELLER: { text: "Bán chạy", tone: "default" },
  SOLD_OUT: { text: "Hết hàng", tone: "default" },
};

/** Tên màu trong option_values → màu chấm trên thẻ (UI-07). Tên lạ dùng xám trung tính. */
const COLOR_HEX: Record<string, string> = {
  "đen": "#111111",
  "trắng": "#f3efe8",
  "be": "#cfc6ba",
  "kem": "#efe9dd",
  "nâu": "#6b4a2b",
  "xám": "#8a8a8a",
  "xanh rêu": "#2f3a2f",
  "xanh navy": "#1f2a44",
  "hồng đất": "#d8c3b8",
  "đỏ": "#8c2b2b",
  "bạc": "#d7d7d7",
  "mạ vàng": "#d9b65c",
  "vàng": "#d9b65c",
  "vàng hồng": "#d9a08f",
};

export function colorHex(name: string): string {
  return COLOR_HEX[name.trim().toLowerCase()] ?? "#c9c4bb";
}

export function toCardData(view: ProductCardView): ProductCardData {
  const label = view.label ? LABEL_TEXT[view.label] : undefined;
  return {
    slug: view.slug,
    name: view.name,
    price: view.price ?? 0,
    compareAtPrice: view.compareAtPrice ?? undefined,
    imageUrl: view.imageUrl ?? undefined,
    colors: view.colors.map(colorHex),
    label: label?.text,
    labelTone: label?.tone,
    tone: toneFor(view.slug),
    icon: iconFor(view.slug),
    soldOut: view.label === "SOLD_OUT",
  };
}

/** Khi chưa có ảnh thật: chọn tông và nét vẽ placeholder theo slug để lưới không đơn điệu. */
export function iconFor(slug: string): IconName | undefined {
  if (/nhan/.test(slug)) return "ring";
  if (/day-chuyen|vong-co/.test(slug)) return "necklace";
  if (/khuyen-tai|bong-tai/.test(slug)) return "earring";
  if (/tui/.test(slug)) return "bag";
  if (/vay|dam|quan/.test(slug)) return "dress";
  if (/ao/.test(slug)) return "shirt";
  return undefined;
}

const TONES: Tone[] = ["stone", "sand", "bone", "taupe"];

export function toneFor(slug: string): Tone {
  let hash = 0;
  for (const ch of slug) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  return TONES[hash % TONES.length];
}

/** Danh mục lá để hiển thị ô "Mua theo danh mục" (tối đa 6). */
export function leafCategories(tree: CategoryTreeNode[], limit = 6): { slug: string; name: string }[] {
  const leaves: { slug: string; name: string }[] = [];
  const walk = (nodes: CategoryTreeNode[]) => {
    for (const node of nodes) {
      if (node.children.length === 0) leaves.push({ slug: node.slug, name: node.name });
      else walk(node.children);
    }
  };
  walk(tree);
  return leaves.slice(0, limit);
}

/** Tìm nút danh mục theo slug kèm chuỗi cha (breadcrumb). */
export function findCategory(tree: CategoryTreeNode[], slug: string): { node: CategoryTreeNode; trail: CategoryTreeNode[] } | null {
  const walk = (nodes: CategoryTreeNode[], trail: CategoryTreeNode[]): { node: CategoryTreeNode; trail: CategoryTreeNode[] } | null => {
    for (const node of nodes) {
      if (node.slug === slug) return { node, trail };
      const found = walk(node.children, [...trail, node]);
      if (found) return found;
    }
    return null;
  };
  return walk(tree, []);
}

/** Tên option kỹ thuật → nhãn hiển thị tiếng Việt. */
export const OPTION_LABEL: Record<string, string> = {
  size: "Size",
  color: "Màu",
  length: "Chiều dài",
  material: "Chất liệu",
};

export function optionLabel(name: string): string {
  return OPTION_LABEL[name] ?? name;
}
