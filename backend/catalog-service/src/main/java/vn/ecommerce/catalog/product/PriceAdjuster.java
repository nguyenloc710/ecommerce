package vn.ecommerce.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * SPI để module khác (flashsale) điều chỉnh giá bán đang hiệu lực của SKU mà module product không phải phụ thuộc
 * ngược vào chúng (đảo phụ thuộc, tránh vòng giữa các module). Giá điều chỉnh áp vào trang sản phẩm, danh sách và
 * {@link VariantInfo} (order-service tra giá lúc checkout); giá gốc của variant thành giá gạch.
 */
public interface PriceAdjuster {

    /** Giá đang hiệu lực theo SKU; SKU không có trong kết quả thì giữ giá gốc. */
    Map<UUID, AdjustedPrice> currentPrices(Collection<UUID> skuIds);

    /**
     * @param label   mã nhãn cho FE dịch (ví dụ {@code FLASH_SALE})
     * @param endsAt  hết hiệu lực lúc nào (FE đếm ngược); null nếu không giới hạn
     */
    record AdjustedPrice(BigDecimal price, String label, Instant endsAt) {
    }
}
