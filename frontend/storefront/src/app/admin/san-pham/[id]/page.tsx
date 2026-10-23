import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { AdminHeader } from "@/components/admin/admin-ui";
import { ImagesEditor } from "@/components/admin/images-editor";
import { ProductForm } from "@/components/admin/product-form";
import { VariantsEditor } from "@/components/admin/variants-editor";
import { formatDateTime } from "@/components/order/order-items";
import { getAdminProduct, listBrands, listCategories } from "@/lib/admin-data";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Sửa sản phẩm" };

export default function Page({ params }: PageProps<"/admin/san-pham/[id]">) {
  return (
    <Suspense fallback={<div className="card h-64 animate-pulse" />}>
      <EditProduct params={params} />
    </Suspense>
  );
}

async function EditProduct({ params }: Pick<PageProps<"/admin/san-pham/[id]">, "params">) {
  const { id } = await params;
  await requireStaff(`/admin/san-pham/${id}`);
  const [product, categories, brands] = await Promise.all([getAdminProduct(id), listCategories(), listBrands()]);
  return (
    <>
      <AdminHeader
        title={product.name}
        back={{ href: "/admin/san-pham", label: "Sản phẩm" }}
        actions={
          product.status === "ACTIVE" ? (
            <Link href={`/san-pham/${product.slug}`} target="_blank" className="btn-admin btn-admin-light">
              Xem trên cửa hàng ↗
            </Link>
          ) : null
        }
      />
      <div className="flex flex-col gap-4">
        {/* Không key theo version: refresh sau khi lưu đưa version mới vào props, form giữ trạng thái "Đã lưu" */}
        <ProductForm product={product} categories={categories} brands={brands} />
        <VariantsEditor productId={product.id} variants={product.variants} goldPricing={product.pricingType === "GOLD_RATE"} />
        {/* key theo id ảnh: ảnh mới có id sau khi lưu, phải nạp lại để lần lưu sau không tạo trùng */}
        <ImagesEditor key={product.images.map((i) => i.id).join()} productId={product.id} images={product.images} variants={product.variants} />
        <p className="m-0 text-[12px] text-muted">Cập nhật lần cuối {formatDateTime(product.updatedAt)}</p>
      </div>
    </>
  );
}
