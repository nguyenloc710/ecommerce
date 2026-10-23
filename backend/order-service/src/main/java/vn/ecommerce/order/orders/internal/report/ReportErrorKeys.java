package vn.ecommerce.order.orders.internal.report;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportErrorKeys {

    /** Thiếu ngày, từ ngày sau đến ngày, hoặc dài quá {0} ngày. */
    public static final String INVALID_RANGE = "report.invalid-range";
}
