package vn.ecommerce.catalog.inventory.internal.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.catalog.inventory.internal.domain.InventoryItem;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findBySkuIdIn(Collection<UUID> skuIds);

    Optional<InventoryItem> findBySkuId(UUID skuId);

    /**
     * Trang tồn kho cho admin. {@code anySku=false} giới hạn theo danh sách id (đã lọc theo từ khóa ở module product);
     * {@code lowStockOnly} chỉ lấy SKU có available <= ngưỡng.
     */
    @Query(value = """
            select i from InventoryItem i
            where (:anySku = true or i.skuId in :skuIds)
              and (:lowStockOnly = false or (i.onHand - i.reserved) <= i.lowStockThreshold)
            order by (i.onHand - i.reserved) asc, i.updatedAt desc
            """, countQuery = """
            select count(i) from InventoryItem i
            where (:anySku = true or i.skuId in :skuIds)
              and (:lowStockOnly = false or (i.onHand - i.reserved) <= i.lowStockThreshold)
            """)
    Page<InventoryItem> pageAdmin(@Param("anySku") boolean anySku, @Param("skuIds") Collection<UUID> skuIds,
                                  @Param("lowStockOnly") boolean lowStockOnly, Pageable pageable);
}
