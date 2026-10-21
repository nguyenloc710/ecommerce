import type { ProductDetail } from "@/lib/catalog-shared";
import { SHOP_NAME, absoluteUrl } from "@/lib/site";

/**
 * Dữ liệu có cấu trúc schema.org cho trang sản phẩm: Product + AggregateOffer (khoảng giá các biến thể) và
 * BreadcrumbList. Không ghi tình trạng còn hàng vì tồn kho đọc lúc request, còn khối này nằm trong phần prerender.
 */
export function ProductJsonLd({ product }: { product: ProductDetail }) {
  const prices = product.variants.map((v) => v.price);
  const url = absoluteUrl(`/san-pham/${product.slug}`);
  const data = [
    {
      "@context": "https://schema.org",
      "@type": "Product",
      name: product.name,
      description: product.description ?? undefined,
      image: product.images.map((i) => absoluteUrl(i.url)),
      sku: product.variants[0]?.skuCode,
      brand: { "@type": "Brand", name: product.brand?.name ?? SHOP_NAME },
      category: product.category.name,
      url,
      offers:
        prices.length > 0
          ? {
              "@type": "AggregateOffer",
              priceCurrency: "VND",
              lowPrice: Math.min(...prices),
              highPrice: Math.max(...prices),
              offerCount: prices.length,
              url,
            }
          : undefined,
    },
    {
      "@context": "https://schema.org",
      "@type": "BreadcrumbList",
      itemListElement: [
        { name: "Trang chủ", path: "/" },
        ...product.breadcrumb.map((b) => ({ name: b.name, path: `/danh-muc/${b.slug}` })),
        { name: product.name, path: `/san-pham/${product.slug}` },
      ].map((item, i) => ({ "@type": "ListItem", position: i + 1, name: item.name, item: absoluteUrl(item.path) })),
    },
  ];
  // Thoát "<" để nội dung (tên, mô tả do admin nhập) không đóng được thẻ script
  const json = JSON.stringify(data).replace(/</g, "\\u003c");
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: json }} />;
}
