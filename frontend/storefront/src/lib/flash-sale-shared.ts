/** Kiểu dữ liệu flash sale theo hợp đồng catalog-service (module flashsale), dùng chung server/client. */
import { z } from "zod";

export type CurrentFlashSaleItem = {
  skuId: string;
  productId: string;
  productSlug: string;
  productName: string;
  variantLabel: string;
  imageUrl: string | null;
  /** Giá gạch, cùng cách tính với trang sản phẩm. */
  compareAtPrice: number;
  salePrice: number;
  quota: number;
  /** Phần trăm suất đã bán hoặc đang được giữ cho đơn chưa thanh toán. */
  claimedPercent: number;
  soldOut: boolean;
};

export type CurrentFlashSale = { id: string; name: string; startsAt: string; endsAt: string; items: CurrentFlashSaleItem[] };

export type FlashSalePhase = "UPCOMING" | "RUNNING" | "ENDED" | "CANCELLED";

export type AdminFlashSaleRow = {
  id: string;
  name: string;
  startsAt: string;
  endsAt: string;
  phase: FlashSalePhase;
  itemCount: number;
  totalQuota: number;
  totalSold: number;
  version: number;
};

export type AdminFlashSaleItem = {
  itemId: string;
  skuId: string;
  skuCode: string | null;
  productName: string | null;
  productSlug: string | null;
  variantLabel: string | null;
  imageUrl: string | null;
  listPrice: number | null;
  salePrice: number;
  quota: number;
  sold: number;
  held: number;
  perCustomerLimit: number;
};

export type AdminFlashSaleDetail = {
  id: string;
  name: string;
  startsAt: string;
  endsAt: string;
  phase: FlashSalePhase;
  version: number;
  items: AdminFlashSaleItem[];
};

export const PHASE_TEXT: Record<FlashSalePhase, string> = {
  UPCOMING: "Sắp diễn ra",
  RUNNING: "Đang chạy",
  ENDED: "Đã kết thúc",
  CANCELLED: "Đã dừng",
};

/** Màu nhãn trạng thái ở trang quản trị (tone của Badge). */
export const PHASE_TONE: Record<FlashSalePhase, "info" | "success" | "neutral" | "critical"> = {
  UPCOMING: "info",
  RUNNING: "success",
  ENDED: "neutral",
  CANCELLED: "critical",
};

/** Khớp ecommerce.flash-sale.max-items-per-sale của catalog-service (server vẫn kiểm tra lại). */
export const FLASH_SALE_MAX_ITEMS = 50;

// ---- Form tạo chương trình (khớp FlashSaleDtos.CreateCommand) ----

const wholeNumber = (message: string) => z.string().trim().regex(/^\d+$/, message);

export const flashSaleItemSchema = z.object({
  skuId: z.string().min(1),
  // Chỉ để hiển thị trong form, không gửi
  title: z.string(),
  salePrice: wholeNumber("Giá là số nguyên dương").refine((v) => Number(v) > 0, "Giá phải lớn hơn 0"),
  quota: wholeNumber("Số suất là số nguyên").refine((v) => Number(v) >= 1 && Number(v) <= 100_000, "Từ 1 đến 100.000 suất"),
  perCustomerLimit: wholeNumber("Nhập 0 nếu không giới hạn").refine((v) => Number(v) <= 999, "Tối đa 999"),
});

/** {@code startsAt}/{@code endsAt}: giá trị datetime-local ở form, ISO (UTC) khi gửi lên server. */
export const flashSaleSchema = z
  .object({
    name: z.string().trim().min(1, "Vui lòng nhập tên chương trình").max(150, "Tối đa 150 ký tự"),
    startsAt: z.string().min(1, "Chọn giờ bắt đầu"),
    endsAt: z.string().min(1, "Chọn giờ kết thúc"),
    items: z.array(flashSaleItemSchema).min(1, "Thêm ít nhất một SKU").max(FLASH_SALE_MAX_ITEMS, `Tối đa ${FLASH_SALE_MAX_ITEMS} SKU`),
  })
  .refine((v) => new Date(v.endsAt).getTime() > new Date(v.startsAt).getTime(), { path: ["endsAt"], message: "Giờ kết thúc phải sau giờ bắt đầu" });

export type FlashSaleFormValues = z.infer<typeof flashSaleSchema>;
