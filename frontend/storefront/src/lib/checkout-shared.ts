/**
 * Kiểu dữ liệu theo hợp đồng order-service/customer-service và schema Zod của form thanh toán.
 * Dùng chung cho Client Component (form) và Server Action (kiểm tra lại trước khi gọi API).
 */
import { z } from "zod";

export type Division = { code: string; name: string };

export type SavedAddress = {
  id: string;
  recipientName: string;
  phone: string;
  provinceCode: string;
  provinceName: string;
  wardCode: string;
  wardName: string;
  line1: string;
  note: string | null;
  isDefault: boolean;
};

export type ShippingOption = { carrier: string; carrierName: string; fee: number; etaDays: number };

export type CheckoutQuote = {
  lines: { skuId: string; productName: string; variantLabel: string; imageUrl: string | null; unitPrice: number; quantity: number; engraving: string | null; lineTotal: number }[];
  subtotal: number;
  voucherCode: string | null;
  discount: number;
  voucherError: string | null;
  shippingOptions: ShippingOption[];
  selectedShipping: ShippingOption | null;
  shippingFee: number;
  total: number;
};

export type OrderStatus =
  | "PENDING"
  | "AWAITING_PAYMENT"
  | "CONFIRMED"
  | "PACKING"
  | "SHIPPING"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED"
  | "RETURN_REQUESTED"
  | "RETURNED"
  | "REFUNDED";

export type OrderView = {
  id: string;
  code: string;
  status: OrderStatus;
  subtotal: number;
  discount: number;
  shippingFee: number;
  total: number;
  voucherCode: string | null;
  paymentMethod: PaymentMethod;
  shippingCarrier: string;
  shippingAddress: { recipientName: string; phone: string; provinceName: string; wardName: string; line1: string; note: string | null };
  contactEmail: string;
  note: string | null;
  placedAt: string;
  paidAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  items: { skuId: string; skuCode: string; productSlug: string; productName: string; variantLabel: string; imageUrl: string | null; unitPrice: number; quantity: number; engraving: string | null; lineTotal: number }[];
  history: { from: string | null; to: OrderStatus; reason: string | null; at: string }[];
  shipment: { carrier: string; trackingCode: string; status: string } | null;
};

export type OrderSummary = {
  id: string;
  code: string;
  status: OrderStatus;
  total: number;
  paymentMethod: PaymentMethod;
  itemCount: number;
  firstItemName: string;
  firstItemImage: string | null;
  placedAt: string;
};

export const PAYMENT_METHODS = ["COD", "MOCK_ONLINE"] as const;
export type PaymentMethod = (typeof PAYMENT_METHODS)[number];

export const PAYMENT_LABELS: Record<PaymentMethod, { title: string; hint: string }> = {
  COD: { title: "Thanh toán khi nhận hàng (COD)", hint: "Trả tiền mặt cho nhân viên giao hàng" },
  MOCK_ONLINE: { title: "Thanh toán online", hint: "Cổng thanh toán thử nghiệm (VNPay/MoMo sẽ thay ở bản chính thức)" },
};

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: "Chờ xử lý",
  AWAITING_PAYMENT: "Chờ thanh toán",
  CONFIRMED: "Đã xác nhận",
  PACKING: "Đang đóng gói",
  SHIPPING: "Đang giao",
  DELIVERED: "Đã giao",
  COMPLETED: "Hoàn tất",
  CANCELLED: "Đã hủy",
  RETURN_REQUESTED: "Yêu cầu đổi trả",
  RETURNED: "Đã trả hàng",
  REFUNDED: "Đã hoàn tiền",
};

/** Khách tự hủy được ở các trạng thái này (OrderStatus.customerCancellable ở order-service). */
export const CUSTOMER_CANCELLABLE: OrderStatus[] = ["PENDING", "AWAITING_PAYMENT", "CONFIRMED"];

/** Voucher lỗi trong quote chỉ trả message key; lần áp dụng đầu đã có message dịch sẵn từ /api/vouchers/validate. */
export const VOUCHER_ERROR_FALLBACK = "Mã giảm giá không còn áp dụng được cho đơn này.";

const PHONE = /^(0|\+84)\d{9}$/;

/** Khớp CheckoutRequest.AddressInput / AddressUpsertCommand (tên tỉnh/phường tra từ danh mục khi gửi). */
export const addressSchema = z.object({
  recipientName: z.string().trim().min(1, "Vui lòng nhập họ tên người nhận").max(150, "Tối đa 150 ký tự"),
  phone: z.string().trim().regex(PHONE, "Số điện thoại gồm 10 số, bắt đầu bằng 0 hoặc +84"),
  provinceCode: z.string().min(1, "Vui lòng chọn tỉnh/thành phố"),
  wardCode: z.string().min(1, "Vui lòng chọn phường/xã"),
  line1: z.string().trim().min(1, "Vui lòng nhập số nhà, tên đường").max(255, "Tối đa 255 ký tự"),
  note: z.string().trim().max(255, "Tối đa 255 ký tự").optional(),
});
export type AddressInput = z.infer<typeof addressSchema>;

export const checkoutSchema = z
  .object({
    addressId: z.string().optional(),
    address: addressSchema.optional(),
    saveAddress: z.boolean().optional(),
    contactEmail: z.email("Email không hợp lệ").max(255),
    shippingCarrier: z.string().max(20).optional(),
    paymentMethod: z.enum(PAYMENT_METHODS, "Vui lòng chọn phương thức thanh toán"),
    voucherCode: z.string().trim().max(30).optional(),
    note: z.string().trim().max(500, "Tối đa 500 ký tự").optional(),
  })
  .refine((v) => Boolean(v.addressId) || Boolean(v.address), { message: "Vui lòng chọn hoặc nhập địa chỉ giao hàng", path: ["addressId"] });
export type CheckoutInput = z.infer<typeof checkoutSchema>;

export type CheckoutLine = { skuId: string; quantity: number; engraving?: string };

/**
 * Schema của form (client): địa chỉ chọn từ sổ hoặc nhập mới. onSubmit đổi sang CheckoutInput để gửi Server Action.
 */
export const checkoutFormSchema = z
  .object({
    addressMode: z.enum(["saved", "new"]),
    addressId: z.string().optional(),
    address: z.object({
      recipientName: z.string(),
      phone: z.string(),
      provinceCode: z.string(),
      wardCode: z.string(),
      line1: z.string(),
      note: z.string().optional(),
    }),
    saveAddress: z.boolean(),
    contactEmail: z.email("Email không hợp lệ").max(255),
    shippingCarrier: z.string().max(20).optional(),
    paymentMethod: z.enum(PAYMENT_METHODS, "Vui lòng chọn phương thức thanh toán"),
    note: z.string().trim().max(500, "Tối đa 500 ký tự").optional(),
  })
  .superRefine((v, ctx) => {
    if (v.addressMode === "saved") {
      if (!v.addressId) ctx.addIssue({ code: "custom", path: ["addressId"], message: "Vui lòng chọn địa chỉ giao hàng" });
      return;
    }
    const r = addressSchema.safeParse(v.address);
    if (!r.success) for (const i of r.error.issues) ctx.addIssue({ code: "custom", path: ["address", ...i.path], message: i.message });
  });
export type CheckoutFormValues = z.infer<typeof checkoutFormSchema>;

/** Lý do hủy do hệ thống ghi (OrderServiceImpl.REASON_*) → câu hiển thị; lý do khách tự nhập giữ nguyên. */
export function cancelReasonLabel(reason: string): string {
  if (reason === "customer-cancel") return "Bạn đã hủy đơn";
  if (reason === "payment-timeout") return "Quá hạn thanh toán";
  if (reason.startsWith("payment-failed")) return "Thanh toán không thành công";
  return reason;
}
