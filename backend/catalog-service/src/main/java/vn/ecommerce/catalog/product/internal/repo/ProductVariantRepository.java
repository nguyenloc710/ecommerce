package vn.ecommerce.catalog.product.internal.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.catalog.product.internal.domain.GoldKarat;
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

    List<ProductVariant> findByProductIdOrderBySortOrderAsc(UUID productId);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);

    boolean existsBySkuCode(String skuCode);

    boolean existsBySkuCodeAndIdNot(String skuCode, UUID id);

    /** Variant tính giá theo vàng loại {@code karat} (sản phẩm GOLD_RATE, đủ trường giá vàng) để tính lại khi có giá mới. */
    @Query("""
            select v from ProductVariant v join fetch v.product p
            where v.goldKarat = :karat and v.goldWeight is not null and v.makingFee is not null
              and p.pricingType = vn.ecommerce.catalog.product.internal.domain.PricingType.GOLD_RATE
            """)
    List<ProductVariant> findGoldPricedByKarat(@Param("karat") GoldKarat karat);

    // ---- tra cứu cho module khác ----

    @Query("""
            select new vn.ecommerce.catalog.product.internal.repo.VariantInfoRow(
                v.id, p.id, p.slug, p.name, v.skuCode, v.optionValues, v.price, v.compareAtPrice, v.status, p.status)
            from ProductVariant v join v.product p
            where v.id in :ids
            """)
    List<VariantInfoRow> findInfoRowsByIdIn(@Param("ids") Collection<UUID> ids);

    @Query("""
            select v.id from ProductVariant v join v.product p
            where lower(v.skuCode) like :q or lower(p.name) like :q
            order by p.name, v.sortOrder
            """)
    List<UUID> searchIds(@Param("q") String q, Pageable limit);
}
