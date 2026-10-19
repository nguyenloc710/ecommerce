package vn.ecommerce.common.web.error;

import java.time.Instant;
import java.util.List;

/**
 * Body lỗi chuẩn trả về cho client ở mọi service.
 *
 * @param code    mã lỗi ổn định để FE xử lý theo nhánh (ví dụ {@code order.out-of-stock})
 * @param message thông điệp đã dịch theo {@code Accept-Language}
 * @param traceId mã trace để tra log; khách báo lỗi kèm mã này
 * @param errors  lỗi theo field khi validate request thất bại, rỗng nếu không có
 */
public record ErrorResponse(
        String code,
        String message,
        String traceId,
        Instant timestamp,
        List<FieldViolation> errors) {

    public record FieldViolation(String field, String message) {
    }
}
