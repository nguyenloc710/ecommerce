package vn.ecommerce.order.orders.internal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** B-02: {@code ORD-yymmdd-NNNN}; NNNN tăng theo ngày bằng một UPSERT nguyên tử trên {@code order_code_seq}. */
@Component
@RequiredArgsConstructor
class OrderCodeGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");

    private final JdbcTemplate jdbc;
    private final OrderProperties properties;

    String next() {
        LocalDate today = LocalDate.now(properties.zoneId());
        Integer no = jdbc.queryForObject("""
                insert into order_code_seq (day, last_no) values (?, 1)
                on conflict (day) do update set last_no = order_code_seq.last_no + 1
                returning last_no
                """, Integer.class, today);
        return "ORD-" + DAY.format(today) + "-" + String.format("%04d", no);
    }
}
