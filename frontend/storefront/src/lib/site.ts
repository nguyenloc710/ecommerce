/** URL public của storefront (metadataBase, sitemap, JSON-LD, URL quay về sau đăng xuất). */
export const SITE_URL = (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/$/, "");

export const SHOP_NAME = process.env.NEXT_PUBLIC_SHOP_NAME ?? "Tên shop";

/** Đường dẫn tương đối (ảnh /img/seed/..., trang) → URL tuyệt đối; URL đã tuyệt đối giữ nguyên. */
export const absoluteUrl = (path: string) => (/^https?:\/\//.test(path) ? path : `${SITE_URL}${path.startsWith("/") ? "" : "/"}${path}`);
