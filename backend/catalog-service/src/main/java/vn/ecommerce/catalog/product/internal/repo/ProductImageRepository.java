package vn.ecommerce.catalog.product.internal.repo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.catalog.product.internal.domain.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    @Query("""
            select new vn.ecommerce.catalog.product.internal.repo.ImageRow(i.id, i.product.id, i.variant.id, i.url, i.alt, i.sortOrder)
            from ProductImage i
            where i.product.id in :productIds
            order by i.product.id, i.sortOrder
            """)
    List<ImageRow> findRowsByProductIdIn(@Param("productIds") Collection<UUID> productIds);
}
