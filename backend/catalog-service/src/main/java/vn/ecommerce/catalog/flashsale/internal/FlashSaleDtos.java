package vn.ecommerce.catalog.flashsale.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Hợp đồng API của module flashsale. */
public final class FlashSaleDtos {

    private FlashSaleDtos() {
    }

    /** Số SKU tối đa kiểm tra lại theo {@code ecommerce.flash-sale.max-items-per-sale}. */
    public record CreateCommand(
            @NotBlank(message = "{validation.required}") @Size(max = 150, message = "{validation.max-length}") String name,
            @NotNull(message = "{validation.required}") Instant startsAt,
            @NotNull(message = "{validation.required}") Instant endsAt,
            @NotEmpty(message = "{validation.required}") List<@Valid ItemCommand> items) {
    }

    /** @param perCustomerLimit 0 = không giới hạn số suất mỗi khách */
    public record ItemCommand(
            @NotNull(message = "{validation.required}") UUID skuId,
            @NotNull(message = "{validation.required}") @Positive(message = "{validation.min}") BigDecimal salePrice,
            @Min(value = 1, message = "{validation.min}") @Max(value = 100_000, message = "{validation.max}") int quota,
            @Min(value = 0, message = "{validation.min}") @Max(value = 999, message = "{validation.max}") int perCustomerLimit) {
    }

    public record CancelCommand(@NotNull(message = "{validation.required}") Long version) {
    }

    /** @param phase UPCOMING | RUNNING | ENDED | CANCELLED */
    public record FlashSaleRow(UUID id, String name, Instant startsAt, Instant endsAt, String phase, int itemCount, long totalQuota,
                               long totalSold, long version) {
    }

    public record FlashSaleDetail(UUID id, String name, Instant startsAt, Instant endsAt, String phase, long version,
                                  List<ItemRow> items) {
    }

    /** @param held suất đơn chưa chốt đang giữ */
    public record ItemRow(UUID itemId, UUID skuId, String skuCode, String productName, String productSlug, String variantLabel,
                          String imageUrl, BigDecimal listPrice, BigDecimal salePrice, int quota, int sold, long held,
                          int perCustomerLimit) {
    }

    /** Chương trình đang chạy cho trang chủ / trang flash sale. */
    public record CurrentFlashSale(UUID id, String name, Instant startsAt, Instant endsAt, List<CurrentItem> items) {
    }

    /**
     * @param compareAtPrice giá gạch, cùng cách tính với trang sản phẩm (giá gạch gốc nếu cao hơn, không thì giá niêm yết)
     * @param claimedPercent phần trăm suất đã bán hoặc đang giữ (thanh tiến độ "Đã bán")
     * @param soldOut        hết suất: trang sản phẩm đã quay về giá thường
     */
    public record CurrentItem(UUID skuId, UUID productId, String productSlug, String productName, String variantLabel, String imageUrl,
                              BigDecimal compareAtPrice, BigDecimal salePrice, int quota, int claimedPercent, boolean soldOut) {
    }
}
