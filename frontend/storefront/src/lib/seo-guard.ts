/**
 * Kiểm tra slug sản phẩm/danh mục có tồn tại, dùng trong proxy để URL sai trả HTTP 404 thật.
 * Lý do: bật Partial Prefetching nên URL ngoài generateStaticParams nhận App Shell (200) ngay rồi mới render,
 * notFound() trong trang lúc đó chỉ còn là 200 + noindex. Proxy chạy trước khi render nên còn đặt được status.
 *
 * Cache trong bộ nhớ tiến trình (proxy không dùng được "use cache"); lỗi mạng/5xx → coi như tồn tại (không chặn nhầm).
 */
const API_BASE = process.env.API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const TTL_MS = 60_000;
const MAX_ENTRIES = 5_000;
const TIMEOUT_MS = 3_000;

const productCache = new Map<string, { exists: boolean; expires: number }>();
let categoryCache: { slugs: Set<string>; expires: number } | null = null;

type CategoryNode = { slug: string; children: CategoryNode[] };

export async function productExists(slug: string): Promise<boolean> {
  const hit = productCache.get(slug);
  if (hit && hit.expires > Date.now()) return hit.exists;
  let exists = true;
  try {
    const res = await fetch(`${API_BASE}/api/products/${encodeURIComponent(slug)}`, { signal: AbortSignal.timeout(TIMEOUT_MS) });
    exists = res.status !== 404;
  } catch {
    return true;
  }
  if (productCache.size >= MAX_ENTRIES) productCache.clear();
  productCache.set(slug, { exists, expires: Date.now() + TTL_MS });
  return exists;
}

export async function categoryExists(slug: string): Promise<boolean> {
  if (!categoryCache || categoryCache.expires <= Date.now()) {
    try {
      const res = await fetch(`${API_BASE}/api/categories`, { signal: AbortSignal.timeout(TIMEOUT_MS) });
      if (!res.ok) return true;
      const tree = (await res.json()) as CategoryNode[];
      const walk = (nodes: CategoryNode[]): string[] => nodes.flatMap((n) => [n.slug, ...walk(n.children)]);
      categoryCache = { slugs: new Set(walk(tree)), expires: Date.now() + TTL_MS };
    } catch {
      return true;
    }
  }
  return categoryCache.slugs.has(slug);
}
