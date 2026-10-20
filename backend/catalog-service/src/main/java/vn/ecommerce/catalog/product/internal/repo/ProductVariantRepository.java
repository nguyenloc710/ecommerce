package vn.ecommerce.catalog.product.internal.repo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;
import vn.ecommerce.catalog.product.internal.domain.VariantStatus;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    @Query("""
            select new vn.ecommerce.catalog.product.internal.repo.VariantRow(v.product.id, v.id, v.price, v.compareAtPrice, v.optionValues)
            from ProductVariant v
            where v.product.id in :productIds and v.status = vn.ecommerce.catalog.product.internal.domain.VariantStatus.ACTIVE
            order by v.sortOrder
            """)
    List<VariantRow> findActiveRowsByProductIdIn(@Param("productIds") Collection<UUID> productIds);

    List<ProductVariant> findByProductIdAndStatusOrderBySortOrderAsc(UUID productId, VariantStatus status);
}
