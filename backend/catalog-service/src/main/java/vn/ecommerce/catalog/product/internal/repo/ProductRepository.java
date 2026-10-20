package vn.ecommerce.catalog.product.internal.repo;

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
 * Truy vấn entity/projection cơ bản. Danh sách công khai (tìm kiếm, lọc, facet) nằm ở {@link ProductSearchRepository}.
 * Tham số {@code anyStatus}/{@code anyCategory} thay cho {@code :x is null} vì PostgreSQL không suy được kiểu
 * của tham số UUID/enum null.
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

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
