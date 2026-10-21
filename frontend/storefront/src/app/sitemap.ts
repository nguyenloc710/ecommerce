import type { MetadataRoute } from "next";
import { listAllCategorySlugs, listAllProductSlugs } from "@/lib/catalog";
import { SITE_URL } from "@/lib/site";

/** /sitemap.xml: trang chính, mọi danh mục và sản phẩm đang bán (dữ liệu catalog có cache theo giờ). */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const [categories, products] = await Promise.all([listAllCategorySlugs(), listAllProductSlugs()]);
  return [
    { url: `${SITE_URL}/`, changeFrequency: "daily", priority: 1 },
    { url: `${SITE_URL}/moi-ve`, changeFrequency: "daily", priority: 0.8 },
    { url: `${SITE_URL}/sale`, changeFrequency: "daily", priority: 0.7 },
    ...categories.map((slug) => ({ url: `${SITE_URL}/danh-muc/${slug}`, changeFrequency: "daily" as const, priority: 0.8 })),
    ...products.map((slug) => ({ url: `${SITE_URL}/san-pham/${slug}`, changeFrequency: "weekly" as const, priority: 0.6 })),
  ];
}
