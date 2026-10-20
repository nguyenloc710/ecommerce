package vn.ecommerce.catalog.product.internal.repo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.CategoryStatus;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("""
            select new vn.ecommerce.catalog.product.internal.repo.CategoryRow(c.id, c.parent.id, c.name, c.slug, c.sortOrder)
            from Category c
            where c.status = :status
            order by c.sortOrder, c.name
            """)
    List<CategoryRow> findRowsByStatus(@Param("status") CategoryStatus status);
}
