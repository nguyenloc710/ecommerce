package vn.ecommerce.catalog.flashsale.internal.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, UUID> {

    List<FlashSaleItem> findByFlashSaleIdIn(Collection<UUID> flashSaleIds);

    /** Các SKU đang trong chương trình chạy lúc {@code now}; mỗi SKU tối đa một dòng (tạo chương trình đã chặn trùng giờ). */
    @Query("""
            select new vn.ecommerce.catalog.flashsale.internal.domain.ActiveItem(i.id, i.skuId, i.salePrice, i.quota, i.sold,
                i.perCustomerLimit, s.endsAt,
                coalesce((select sum(h.quantity) from FlashSaleHold h
                          where h.flashSaleItemId = i.id and h.status = vn.ecommerce.catalog.flashsale.internal.domain.HoldStatus.HELD), 0L))
            from FlashSaleItem i join FlashSale s on s.id = i.flashSaleId
            where i.skuId in :skuIds and s.status = vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleStatus.ACTIVE
              and s.startsAt <= :now and s.endsAt > :now
            """)
    List<ActiveItem> findActiveBySkuIdIn(@Param("skuIds") Collection<UUID> skuIds, @Param("now") Instant now);

    /** SKU đã nằm trong chương trình khác (chưa hủy) có khung giờ giao với [startsAt, endsAt). */
    @Query("""
            select distinct i.skuId from FlashSaleItem i join FlashSale s on s.id = i.flashSaleId
            where i.skuId in :skuIds and s.status = vn.ecommerce.catalog.flashsale.internal.domain.FlashSaleStatus.ACTIVE
              and s.startsAt < :endsAt and s.endsAt > :startsAt
            """)
    List<UUID> findOverlappingSkuIds(@Param("skuIds") Collection<UUID> skuIds, @Param("startsAt") Instant startsAt,
                                     @Param("endsAt") Instant endsAt);

    /** Tăng đã bán nguyên tử, không vượt quota; trả 0 nếu vượt (báo động bán quá suất). */
    @Modifying
    @Query("""
            update FlashSaleItem i set i.sold = i.sold + :quantity, i.updatedAt = :now, i.version = i.version + 1
            where i.id = :id and i.sold + :quantity <= i.quota
            """)
    int incrementSold(@Param("id") UUID id, @Param("quantity") int quantity, @Param("now") Instant now);

    @Modifying
    @Query("""
            update FlashSaleItem i set i.sold = i.sold - :quantity, i.updatedAt = :now, i.version = i.version + 1
            where i.id = :id and i.sold >= :quantity
            """)
    int decrementSold(@Param("id") UUID id, @Param("quantity") int quantity, @Param("now") Instant now);
}
