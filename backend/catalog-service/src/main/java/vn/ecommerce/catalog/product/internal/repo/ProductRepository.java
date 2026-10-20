package vn.ecommerce.catalog.product.internal.repo;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminRow;
import vn.ecommerce.catalog.product.internal.domain.Product;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;

/**
 * Trang danh sách có 3 query theo cách sắp xếp (giá cần subquery min variant nên không dùng Sort động).
 * Tham số {@code allCategories}/{@code anyBrand} thay cho {@code :x is null} vì PostgreSQL không suy được kiểu
 * của tham số UUID null.
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    String WHERE = """
            where p.status = vn.ecommerce.catalog.product.internal.domain.ProductStatus.ACTIVE
              and (:allCategories = true or p.category.id in :categoryIds)
              and (:anyBrand = true or p.brand.id = :brandId)
            """;
    String SELECT = "select new vn.ecommerce.catalog.product.internal.repo.ProductRow(p.id, p.slug, p.name, p.publishedAt) from Product p ";
    String COUNT = "select count(p) from Product p ";
    String MIN_PRICE = "(select min(v.price) from ProductVariant v where v.product = p and v.status = vn.ecommerce.catalog.product.internal.domain.VariantStatus.ACTIVE)";

    @Query(value = SELECT + WHERE + " order by p.publishedAt desc nulls last, p.createdAt desc", countQuery = COUNT + WHERE)
    Page<ProductRow> pageNewest(@Param("allCategories") boolean allCategories, @Param("categoryIds") Collection<UUID> categoryIds,
                                @Param("anyBrand") boolean anyBrand, @Param("brandId") UUID brandId, Pageable pageable);

    @Query(value = SELECT + WHERE + " order by " + MIN_PRICE + " asc nulls last, p.createdAt desc", countQuery = COUNT + WHERE)
    Page<ProductRow> pagePriceAsc(@Param("allCategories") boolean allCategories, @Param("categoryIds") Collection<UUID> categoryIds,
                                  @Param("anyBrand") boolean anyBrand, @Param("brandId") UUID brandId, Pageable pageable);

    @Query(value = SELECT + WHERE + " order by " + MIN_PRICE + " desc nulls last, p.createdAt desc", countQuery = COUNT + WHERE)
    Page<ProductRow> pagePriceDesc(@Param("allCategories") boolean allCategories, @Param("categoryIds") Collection<UUID> categoryIds,
                                   @Param("anyBrand") boolean anyBrand, @Param("brandId") UUID brandId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);

    // ---- admin ----

    String ADMIN_WHERE = """
            where (:q is null or lower(p.name) like :q or p.slug like :q)
              and (:anyStatus = true or p.status = :status)
              and (:anyCategory = true or c.id = :categoryId)
            """;

    @Query(value = """
            select new vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminRow(
                p.id, p.slug, p.name, c.name, p.status, p.publishedAt, p.updatedAt, p.version)
            from Product p join p.category c
            """ + ADMIN_WHERE + " order by p.updatedAt desc",
            countQuery = "select count(p) from Product p join p.category c " + ADMIN_WHERE)
    Page<ProductAdminRow> pageAdmin(@Param("q") String q, @Param("anyStatus") boolean anyStatus, @Param("status") ProductStatus status,
                                    @Param("anyCategory") boolean anyCategory, @Param("categoryId") UUID categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findWithRelationsById(UUID id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    long countByCategoryId(UUID categoryId);

    long countByBrandId(UUID brandId);
}
