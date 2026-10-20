package vn.ecommerce.order.orders.internal;

import java.util.List;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.order.orders.OrderStatus;
import vn.ecommerce.order.orders.OrderSummaryView;
import vn.ecommerce.order.orders.OrderView;

/** Thao tác của nhân viên lên đơn; mọi thay đổi ghi lịch sử trạng thái + admin_audit_log. */
public interface OrderAdminService {

    PageResponse<OrderSummaryView> search(OrderStatus status, String q, int page, int size);

    OrderView get(String code);

    /** Chuyển trạng thái thủ công (PACKING, COMPLETED, RETURN_*...); SHIPPING phải đi qua {@link #ship}. */
    OrderView changeStatus(String code, OrderStatus to, String reason);

    /** Tạo vận đơn ở hãng rồi chuyển sang SHIPPING. */
    OrderView ship(String code, String carrier);

    /** Tạo vận đơn hàng loạt; đơn lỗi được bỏ qua và trả về trong kết quả. */
    BulkShipResult bulkShip(List<String> codes, String carrier);

    OrderView cancel(String code, String reason);

    record BulkShipResult(List<String> shipped, List<Failure> failed) {
        public record Failure(String code, String error) {
        }
    }
}
