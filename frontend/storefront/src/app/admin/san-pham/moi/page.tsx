import type { Metadata } from "next";
import { Suspense } from "react";
import { AdminHeader } from "@/components/admin/admin-ui";
import { ProductForm } from "@/components/admin/product-form";
import { listBrands, listCategories } from "@/lib/admin-data";
import { requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Thêm sản phẩm" };

export default function Page() {
  return (
    <>
      <AdminHeader title="Thêm sản phẩm" back={{ href: "/admin/san-pham", label: "Sản phẩm" }} />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <NewProduct />
      </Suspense>
    </>
  );
}

/** Tạo thông tin chung trước (mặc định Nháp), lưu xong chuyển sang trang sửa để thêm biến thể và ảnh. */
async function NewProduct() {
  await requireStaff("/admin/san-pham/moi");
  const [categories, brands] = await Promise.all([listCategories(), listBrands()]);
  return <ProductForm product={null} categories={categories} brands={brands} />;
}
