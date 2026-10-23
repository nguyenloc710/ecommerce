package vn.ecommerce.catalog.flashsale.internal.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashSaleHoldRepository extends JpaRepository<FlashSaleHold, UUID> {

    List<FlashSaleHold> findByOrderId(UUID orderId);

    /** Số suất khách đã giữ hoặc đã mua theo item (giới hạn theo khách, khởi tạo bộ đếm Valkey). */
    @Query("""
            select new vn.ecommerce.catalog.flashsale.internal.domain.ItemQuantity(h.flashSaleItemId, sum(h.quantity))
            from FlashSaleHold h
            where h.flashSaleItemId in :itemIds and h.customerId = :customerId
              and h.status <> vn.ecommerce.catalog.flashsale.internal.domain.HoldStatus.RELEASED
            group by h.flashSaleItemId
            """)
    List<ItemQuantity> sumUsedByCustomer(@Param("itemIds") Collection<UUID> itemIds, @Param("customerId") String customerId);

    /** Số suất đang giữ (đơn chưa chốt) theo item. */
    @Query("""
            select new vn.ecommerce.catalog.flashsale.internal.domain.ItemQuantity(h.flashSaleItemId, sum(h.quantity))
            from FlashSaleHold h
            where h.flashSaleItemId in :itemIds and h.status = vn.ecommerce.catalog.flashsale.internal.domain.HoldStatus.HELD
            group by h.flashSaleItemId
            """)
    List<ItemQuantity> sumHeld(@Param("itemIds") Collection<UUID> itemIds);
}
