package vn.ecommerce.catalog.product.internal.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.ecommerce.catalog.product.internal.domain.Brand;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.Product;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;
import vn.ecommerce.common.jpa.JpaAuditingAutoConfiguration;

/**
 * Chạy trên PostgreSQL thật qua Testcontainers: Flyway tạo schema (gồm bảng Envers), Hibernate validate,
 * kiểm tra query phân trang + sắp xếp theo giá thấp nhất và xóa mềm.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JpaAuditingAutoConfiguration.class)
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration")
class ProductRepositoryDbTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired CategoryRepository categoryRepository;
    @Autowired BrandRepository brandRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;

    private Category jewelry;
    private Category rings;
    private Category fashion;
    private Brand brand;

    @BeforeEach
    void seed() {
        jewelry = categoryRepository.save(new Category("Trang sức", "trang-suc", null));
        rings = categoryRepository.save(new Category("Nhẫn", "nhan", jewelry));
        fashion = categoryRepository.save(new Category("Thời trang", "thoi-trang", null));
        brand = brandRepository.save(new Brand("Nhà may", "nha-may"));

        product(rings, "nhan-re", "Nhẫn rẻ", ProductStatus.ACTIVE, Instant.now().minusSeconds(100), "300000", "350000");
        product(rings, "nhan-dat", "Nhẫn đắt", ProductStatus.ACTIVE, Instant.now(), "900000", "1200000");
        product(fashion, "ao", "Áo", ProductStatus.ACTIVE, Instant.now(), "500000");
        product(rings, "nhan-nhap", "Nhẫn nháp", ProductStatus.DRAFT, null, "100000");
    }

    private Product product(Category category, String slug, String name, ProductStatus status, Instant publishedAt, String... prices) {
        Product p = new Product(category, name, slug);
        p.setBrand(brand);
        p.setStatus(status);
        p.setPublishedAt(publishedAt);
        p.setAttributes(Map.of("Chất liệu", "Bạc 925"));
        productRepository.save(p);
        int i = 0;
        for (String price : prices) {
            ProductVariant v = new ProductVariant(p, slug + "-" + i, new BigDecimal(price));
            v.setOptionValues(Map.of("size", String.valueOf(5 + i), "color", "Bạc"));
            v.setSortOrder(i++);
            variantRepository.save(v);
        }
        return p;
    }

    @Test
    void pageNewest_filtersBySubtree_excludesDraft_andOrdersByPublishedAt() {
        Page<ProductRow> page = productRepository.pageNewest(false, List.of(jewelry.getId(), rings.getId()), true, new UUID(0, 0), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(ProductRow::slug).containsExactly("nhan-dat", "nhan-re");
    }

    @Test
    void pagePriceDesc_ordersByCheapestVariant_acrossAllCategories() {
        Page<ProductRow> page = productRepository.pagePriceDesc(true, List.of(new UUID(0, 0)), true, new UUID(0, 0), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(ProductRow::slug).containsExactly("nhan-dat", "ao", "nhan-re");
    }

    @Test
    void brandFilter_andSoftDelete_areApplied() {
        Brand other = brandRepository.save(new Brand("Khác", "khac"));
        Product hidden = productRepository.findBySlugAndStatus("ao", ProductStatus.ACTIVE).orElseThrow();
        hidden.markDeleted("tester");
        productRepository.saveAndFlush(hidden);

        Page<ProductRow> byBrand = productRepository.pageNewest(true, List.of(new UUID(0, 0)), false, brand.getId(), PageRequest.of(0, 10));
        Page<ProductRow> byOther = productRepository.pageNewest(true, List.of(new UUID(0, 0)), false, other.getId(), PageRequest.of(0, 10));

        assertThat(byBrand.getContent()).extracting(ProductRow::slug).containsExactly("nhan-dat", "nhan-re");
        assertThat(byOther.getTotalElements()).isZero();
        assertThat(productRepository.findBySlugAndStatus("ao", ProductStatus.ACTIVE)).isEmpty();
    }

    @Test
    void variantRows_carryJsonOptionValues() {
        UUID id = productRepository.findBySlugAndStatus("nhan-re", ProductStatus.ACTIVE).orElseThrow().getId();

        List<VariantRow> rows = variantRepository.findActiveRowsByProductIdIn(List.of(id));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).optionValues()).containsEntry("color", "Bạc").containsEntry("size", "5");
        assertThat(rows.get(0).compareAtPrice()).isNull();
    }
}
