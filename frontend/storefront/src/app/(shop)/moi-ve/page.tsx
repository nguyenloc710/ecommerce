import type { Metadata } from "next";
import { ProductListing } from "@/components/listing/product-listing";

export const metadata: Metadata = { title: "Mới về", description: "Sản phẩm mới nhất của mùa này." };

export default function NewArrivalsPage({ searchParams }: PageProps<"/moi-ve">) {
  return (
    <ProductListing
      title="Mới về"
      subtitle="Những thiết kế vừa lên kệ, sắp theo ngày đăng."
      breadcrumb={[{ name: "Mới về" }]}
      fixed={{}}
      basePath="/moi-ve"
      searchParams={searchParams}
    />
  );
}
