package vn.ecommerce.catalog.product.internal.repo;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ecommerce.catalog.product.internal.domain.Brand;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    @Query("select b.id from Brand b where b.slug = :slug")
    Optional<UUID> findIdBySlug(@Param("slug") String slug);
}
