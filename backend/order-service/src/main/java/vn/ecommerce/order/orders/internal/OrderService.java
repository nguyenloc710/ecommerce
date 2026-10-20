package vn.ecommerce.order.orders.internal;

import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.orders.OrderSummaryView;
import vn.ecommerce.order.orders.OrderView;
import vn.ecommerce.order.orders.OrdersApi;
import vn.ecommerce.order.shipping.ShipmentStatusChanged;

/** Đơn hàng phía khách + các luồng tự động (webhook vận chuyển, hết hạn thanh toán). */
public interface OrderService extends OrdersApi {

    PageResponse<OrderSummaryView> listMine(int page, int size);

    OrderView getMine(String code);

    /** Khách tự hủy khi đơn chưa đóng gói ({@link vn.ecommerce.order.orders.OrderStatus#customerCancellable()}). */
    OrderView cancelMine(String code, String reason);

    void onShipmentStatus(ShipmentStatusChanged event);

    /** Hủy đơn AWAITING_PAYMENT quá hạn; trả về số đơn đã hủy. */
    int cancelExpiredAwaitingPayment();
}
