package vn.ecommerce.catalog.product.internal.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.ecommerce.catalog.product.ProductSort;
import vn.ecommerce.catalog.product.internal.domain.Brand;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.Product;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;
import vn.ecommerce.common.jpa.JpaAuditingAutoConfiguration;

/**
 * Chạy trên PostgreSQL thật: Flyway tạo schema (gồm extension unaccent/pg_trgm, cột search_vector, Envers),
 * kiểm tra tìm không dấu, lọc option/giá, facet, sắp xếp và xóa mềm.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JpaAuditingAutoConfiguration.class)
@Import(ProductSearchRepository.class)
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration")
class ProductSearchRepositoryDbTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired CategoryRepository categoryRepository;
    @Autowired BrandRepository brandRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired ProductSearchRepository searchRepository;

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

        product(rings, "nhan-bac-tron", "Nhẫn bạc trơn", ProductStatus.ACTIVE, Instant.now().minusSeconds(100),
                variant("300000", "350000", "6", "Bạc"), variant("300000", null, "7", "Bạc"));
        product(rings, "nhan-vang-18k", "Nhẫn vàng 18K đính đá", ProductStatus.ACTIVE, Instant.now(),
                variant("900000", "1200000", "6", "Vàng"));
        product(fashion, "ao-so-mi-linen", "Áo sơ mi linen", ProductStatus.ACTIVE, Instant.now(),
                variant("500000", null, "M", "Trắng"), variant("500000", null, "L", "Trắng"));
        product(rings, "nhan-nhap", "Nhẫn nháp", ProductStatus.DRAFT, null, variant("100000", null, "6", "Bạc"));
        variantRepository.flush();
    }

    private record V(String price, String compareAt, String size, String color) {
    }

    private static V variant(String price, String compareAt, String size, String color) {
        return new V(price, compareAt, size, color);
    }

    private void product(Category category, String slug, String name, ProductStatus status, Instant publishedAt, V... variants) {
        Product p = new Product(category, name, slug);
        p.setBrand(brand);
        p.setStatus(status);
        p.setPublishedAt(publishedAt);
        p.setAttributes(Map.of("Chất liệu", category == fashion ? "Linen" : "Bạc 925"));
        productRepository.saveAndFlush(p);
        int i = 0;
        for (V v : variants) {
            ProductVariant pv = new ProductVariant(p, slug + "-" + i, new BigDecimal(v.price()));
            pv.setCompareAtPrice(v.compareAt() == null ? null : new BigDecimal(v.compareAt()));
            pv.setOptionValues(Map.of("size", v.size(), "color", v.color()));
            pv.setSortOrder(i++);
            variantRepository.save(pv);
        }
    }

    private static ProductSearchCriteria criteria(String q, Set<UUID> categoryIds, Map<String, List<String>> options, ProductSort sort) {
        return new ProductSearchCriteria(q, categoryIds, null, options, null, null, sort);
    }

    @Test
    void subtreeFilter_excludesDraft_andOrdersNewestFirst() {
        Page<ProductRow> page = searchRepository.search(criteria(null, Set.of(jewelry.getId(), rings.getId()), Map.of(), ProductSort.NEWEST), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(ProductRow::slug).containsExactly("nhan-vang-18k", "nhan-bac-tron");
    }

    @Test
    void searchWithoutDiacritics_matchesVietnameseName_andRanksBestFirst() {
        Page<ProductRow> page = searchRepository.search(criteria("nhan bac", Set.of(), Map.of(), ProductSort.NEWEST), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(ProductRow::slug).containsExactly("nhan-bac-tron");

        Page<ProductRow> partial = searchRepository.search(criteria("so mi", Set.of(), Map.of(), ProductSort.NEWEST), PageRequest.of(0, 10));
        assertThat(partial.getContent()).extracting(ProductRow::slug).containsExactly("ao-so-mi-linen");
    }

    @Test
    void optionAndPriceFilters_applyToActiveVariants() {
        Page<ProductRow> size6 = searchRepository.search(criteria(null, Set.of(), Map.of("size", List.of("6")), ProductSort.PRICE_ASC), PageRequest.of(0, 10));
        assertThat(size6.getContent()).extracting(ProductRow::slug).containsExactly("nhan-bac-tron", "nhan-vang-18k");

        Page<ProductRow> size6Gold = searchRepository.search(criteria(null, Set.of(), Map.of("size", List.of("6"), "color", List.of("Vàng")), ProductSort.NEWEST), PageRequest.of(0, 10));
        assertThat(size6Gold.getContent()).extracting(ProductRow::slug).containsExactly("nhan-vang-18k");

        Page<ProductRow> cheap = searchRepository.search(new ProductSearchCriteria(null, Set.of(), null, Map.of(),
                new BigDecimal("400000"), new BigDecimal("600000"), ProductSort.NEWEST), PageRequest.of(0, 10));
        assertThat(cheap.getContent()).extracting(ProductRow::slug).containsExactly("ao-so-mi-linen");
    }

    @Test
    void priceDesc_ordersByCheapestVariant() {
        Page<ProductRow> page = searchRepository.search(criteria(null, Set.of(), Map.of(), ProductSort.PRICE_DESC), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(ProductRow::slug).containsExactly("nhan-vang-18k", "ao-so-mi-linen", "nhan-bac-tron");
    }

    @Test
    void facets_countProductsPerOptionAndAttribute_withPriceRange() {
        FacetCounts facets = searchRepository.facets(criteria(null, Set.of(jewelry.getId(), rings.getId()), Map.of(), ProductSort.NEWEST));

        assertThat(facets.options().get("size")).containsEntry("6", 2L).containsEntry("7", 1L);
        assertThat(facets.options().get("color")).containsEntry("Bạc", 1L).containsEntry("Vàng", 1L);
        assertThat(facets.attributes().get("Chất liệu")).containsEntry("Bạc 925", 2L);
        assertThat(facets.priceMin()).isEqualByComparingTo("300000");
        assertThat(facets.priceMax()).isEqualByComparingTo("900000");
    }

    @Test
    void brandFilter_andSoftDelete_areApplied() {
        Brand other = brandRepository.save(new Brand("Khác", "khac"));
        Product hidden = productRepository.findBySlugAndStatus("ao-so-mi-linen", ProductStatus.ACTIVE).orElseThrow();
        hidden.markDeleted("tester");
        productRepository.saveAndFlush(hidden);

        Page<ProductRow> byBrand = searchRepository.search(new ProductSearchCriteria(null, Set.of(), brand.getId(), Map.of(), null, null, ProductSort.NEWEST), PageRequest.of(0, 10));
        Page<ProductRow> byOther = searchRepository.search(new ProductSearchCriteria(null, Set.of(), other.getId(), Map.of(), null, null, ProductSort.NEWEST), PageRequest.of(0, 10));

        assertThat(byBrand.getContent()).extracting(ProductRow::slug).containsExactly("nhan-vang-18k", "nhan-bac-tron");
        assertThat(byOther.getTotalElements()).isZero();
        assertThat(productRepository.findBySlugAndStatus("ao-so-mi-linen", ProductStatus.ACTIVE)).isEmpty();
    }

    @Test
    void variantRows_carryJsonOptionValues() {
        UUID id = productRepository.findBySlugAndStatus("nhan-bac-tron", ProductStatus.ACTIVE).orElseThrow().getId();

        List<VariantRow> rows = variantRepository.findActiveRowsByProductIdIn(List.of(id));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).optionValues()).containsEntry("color", "Bạc").containsEntry("size", "6");
        assertThat(rows.get(1).compareAtPrice()).isNull();
    }
}
