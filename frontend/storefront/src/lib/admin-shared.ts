/**
 * Kiểu dữ liệu và schema dùng chung cho trang quản trị (E5.7), theo hợp đồng `/api/admin/**`.
 */
import { z } from "zod";
import type { OrderStatus } from "@/lib/checkout-shared";

export type Paged<T> = { items: T[]; page: number; size: number; total: number; hasNext: boolean };

export type AdminOrderRow = {
  id: string;
  code: string;
  status: OrderStatus;
  total: number;
  paymentMethod: string;
  recipientName: string;
  contactEmail: string;
  itemCount: number;
  firstItemName: string;
  placedAt: string;
};

/** Tab danh sách đơn → filter `status` (rỗng = tất cả). */
export const ORDER_TABS: { key: string; label: string; status?: OrderStatus }[] = [
  { key: "tat-ca", label: "Tất cả" },
  { key: "can-xu-ly", label: "Cần xử lý", status: "CONFIRMED" },
  { key: "dong-goi", label: "Đang đóng gói", status: "PACKING" },
  { key: "dang-giao", label: "Đang giao", status: "SHIPPING" },
  { key: "da-giao", label: "Đã giao", status: "DELIVERED" },
  { key: "cho-thanh-toan", label: "Chờ thanh toán", status: "AWAITING_PAYMENT" },
  { key: "da-huy", label: "Đã hủy", status: "CANCELLED" },
];

export type OrderAction =
  | { kind: "status"; to: OrderStatus; label: string; primary?: boolean }
  | { kind: "ship"; label: string; primary?: boolean }
  | { kind: "cancel"; label: string };

/**
 * Nút thao tác theo trạng thái, chép từ OrderStatus.allowedNext của order-service (API không trả danh sách này).
 * SHIPPING chỉ đi qua /ship (tạo vận đơn), CANCELLED qua /cancel (bắt buộc lý do).
 */
export function orderActions(status: OrderStatus): OrderAction[] {
  switch (status) {
    case "PENDING":
      return [{ kind: "status", to: "CONFIRMED", label: "Xác nhận đơn", primary: true }, { kind: "cancel", label: "Hủy đơn" }];
    case "AWAITING_PAYMENT":
      return [{ kind: "cancel", label: "Hủy đơn" }];
    case "CONFIRMED":
      return [{ kind: "ship", label: "Giao hàng", primary: true }, { kind: "status", to: "PACKING", label: "Chuyển đóng gói" }, { kind: "cancel", label: "Hủy đơn" }];
    case "PACKING":
      return [{ kind: "ship", label: "Giao hàng", primary: true }, { kind: "cancel", label: "Hủy đơn" }];
    case "SHIPPING":
      return [{ kind: "status", to: "DELIVERED", label: "Đánh dấu đã giao", primary: true }];
    case "DELIVERED":
      return [{ kind: "status", to: "COMPLETED", label: "Hoàn tất", primary: true }, { kind: "status", to: "RETURN_REQUESTED", label: "Ghi nhận yêu cầu đổi trả" }];
    case "COMPLETED":
      return [{ kind: "status", to: "RETURN_REQUESTED", label: "Ghi nhận yêu cầu đổi trả" }];
    case "RETURN_REQUESTED":
      return [{ kind: "status", to: "RETURNED", label: "Đã nhận hàng trả", primary: true }, { kind: "status", to: "COMPLETED", label: "Từ chối đổi trả" }];
    case "RETURNED":
      return [{ kind: "status", to: "REFUNDED", label: "Đã hoàn tiền", primary: true }];
    default:
      return [];
  }
}

export const CARRIERS = [
  { code: "GHN", name: "Giao Hàng Nhanh" },
  { code: "GHTK", name: "Giao Hàng Tiết Kiệm" },
];

// ---- Sản phẩm ----

export type ProductStatus = "DRAFT" | "ACTIVE" | "HIDDEN";
export const PRODUCT_STATUS_LABELS: Record<ProductStatus, string> = { DRAFT: "Nháp", ACTIVE: "Đang bán", HIDDEN: "Đã ẩn" };

export type ProductAdminRow = { id: string; slug: string; name: string; categoryName: string; status: ProductStatus; publishedAt: string | null; updatedAt: string; version: number };

export type VariantAdmin = {
  id: string;
  skuCode: string;
  optionValues: Record<string, string>;
  price: number;
  compareAtPrice: number | null;
  weightGram: number | null;
  barcode: string | null;
  status: "ACTIVE" | "HIDDEN";
  sortOrder: number;
  version: number;
};

export type ImageAdmin = { id: string; variantId: string | null; url: string; alt: string | null; sortOrder: number };

export type ProductAdmin = {
  id: string;
  categoryId: string;
  brandId: string | null;
  slug: string;
  name: string;
  description: string | null;
  pricingType: "FIXED" | "GOLD_RATE";
  status: ProductStatus;
  seoTitle: string | null;
  seoDescription: string | null;
  updatedAt: string;
  updatedBy: string | null;
  version: number;
  variants: VariantAdmin[];
  images: ImageAdmin[];
};

export type CategoryOption = { id: string; parentId: string | null; name: string; status: string };
export type BrandOption = { id: string; name: string };

const optionalText = (max: number, msg = `Tối đa ${max} ký tự`) => z.string().trim().max(max, msg).optional();

/** Khớp ProductUpsertCommand (slug để trống thì server sinh từ tên). */
export const productSchema = z.object({
  name: z.string().trim().min(1, "Vui lòng nhập tên sản phẩm").max(200, "Tối đa 200 ký tự"),
  slug: optionalText(160),
  categoryId: z.string().min(1, "Vui lòng chọn danh mục"),
  brandId: z.string().optional(),
  status: z.enum(["DRAFT", "ACTIVE", "HIDDEN"]),
  pricingType: z.enum(["FIXED", "GOLD_RATE"]),
  description: z.string().optional(),
  seoTitle: optionalText(200),
  seoDescription: optionalText(300),
});
export type ProductFormValues = z.infer<typeof productSchema>;

/** Khớp VariantUpsertCommand; option nhập dạng "color=Bạc, size=6". */
export const variantSchema = z.object({
  skuCode: z.string().trim().min(1, "Nhập mã SKU").max(64, "Tối đa 64 ký tự"),
  options: z.string().trim(),
  // Giữ dạng chuỗi trong form (input số), đổi sang number khi gửi
  price: z.string().trim().regex(/^\d+$/, "Giá là số nguyên không âm"),
  compareAtPrice: z.string().trim().regex(/^\d*$/, "Giá gốc là số nguyên không âm"),
  status: z.enum(["ACTIVE", "HIDDEN"]),
});
export type VariantFormValues = z.infer<typeof variantSchema>;

export function parseOptions(raw: string): Record<string, string> {
  const out: Record<string, string> = {};
  for (const part of raw.split(",")) {
    const [k, ...v] = part.split("=");
    const key = k?.trim();
    const value = v.join("=").trim();
    if (key && value) out[key] = value;
  }
  return out;
}

export const formatOptions = (o: Record<string, string>) =>
  Object.entries(o)
    .map(([k, v]) => `${k}=${v}`)
    .join(", ");

// ---- Tồn kho ----

export type InventoryRow = {
  skuId: string;
  skuCode: string;
  productName: string;
  variantLabel: string;
  onHand: number;
  reserved: number;
  available: number;
  lowStockThreshold: number;
  lowStock: boolean;
  version: number;
};

// ---- Đánh giá (E7.1) ----

export type ReviewStatus = "PENDING" | "APPROVED" | "REJECTED";

export const REVIEW_TABS: { status: ReviewStatus; label: string }[] = [
  { status: "PENDING", label: "Chờ duyệt" },
  { status: "APPROVED", label: "Đã duyệt" },
  { status: "REJECTED", label: "Đã từ chối" },
];

export type AdminReviewRow = {
  id: string;
  productId: string;
  productName: string | null;
  productSlug: string | null;
  variantLabel: string | null;
  authorName: string;
  rating: number;
  content: string | null;
  imageUrls: string[];
  status: ReviewStatus;
  rejectReason: string | null;
  createdAt: string;
  moderatedAt: string | null;
};
