/**
 * Ảnh tĩnh cho các khối trang chủ và ô danh mục khi chưa có CMS/ảnh thật (nguồn: public/img/seed/CREDITS.md).
 * Ảnh sản phẩm lấy từ API (product_image), không ở đây.
 */
export const SITE_IMAGES = {
  hero: "/img/seed/site-hero.jpg",
  campaignFashion: "/img/seed/site-campaign-fashion.jpg",
  campaignJewelry: "/img/seed/site-story-workshop.jpg",
  story: "/img/seed/site-campaign-jewelry.jpg",
} as const;

const CATEGORY_IMAGES: Record<string, string> = {
  ao: "/img/seed/ao-so-mi-linen-oversize-2.jpg",
  "vay-quan": "/img/seed/cat-vay-quan.jpg",
  tui: "/img/seed/tui-deo-cheo-da-bo-1.jpg",
  "that-lung": "/img/seed/that-lung-da-ban-nho-1.jpg",
  nhan: "/img/seed/nhan-bac-tron-ban-2mm-1.jpg",
  "day-chuyen": "/img/seed/day-chuyen-bac-mat-da-4-ly-2.jpg",
  "khuyen-tai": "/img/seed/cat-khuyen-tai.jpg",
};

export function categoryImage(slug: string): string | undefined {
  return CATEGORY_IMAGES[slug];
}
