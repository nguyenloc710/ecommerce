package vn.ecommerce.catalog.product.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.ecommerce.catalog.product.CategoryTreeNode;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.ProductCardView;
import vn.ecommerce.catalog.product.ProductListQuery;
import vn.ecommerce.catalog.product.ProductSort;
import vn.ecommerce.catalog.product.internal.domain.CategoryStatus;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;
import vn.ecommerce.catalog.product.internal.repo.BrandRepository;
import vn.ecommerce.catalog.product.internal.repo.CategoryRepository;
import vn.ecommerce.catalog.product.internal.repo.CategoryRow;
import vn.ecommerce.catalog.product.internal.repo.ImageRow;
import vn.ecommerce.catalog.product.internal.repo.ProductImageRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductRow;
import vn.ecommerce.catalog.product.internal.repo.ProductVariantRepository;
import vn.ecommerce.catalog.product.internal.repo.VariantRow;
import vn.ecommerce.common.web.error.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceImplTest {

    private static final UUID ROOT = UUID.randomUUID();
    private static final UUID CHILD = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @Mock CategoryRepository categoryRepository;
    @Mock BrandRepository brandRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock ProductImageRepository imageRepository;

    private ProductQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductQueryServiceImpl(categoryRepository, brandRepository, productRepository, variantRepository,
                imageRepository, Mappers.getMapper(ProductMapper.class), new ProductProperties(Duration.ofDays(30), "color"));
    }

    private void stubCategories() {
        when(categoryRepository.findRowsByStatus(CategoryStatus.ACTIVE)).thenReturn(List.of(
                new CategoryRow(ROOT, null, "Trang sức", "trang-suc", 1),
                new CategoryRow(CHILD, ROOT, "Nhẫn", "nhan", 1),
                new CategoryRow(OTHER, null, "Thời trang", "thoi-trang", 2)));
    }

    @Test
    void categoryTree_nestsChildrenUnderParent() {
        stubCategories();

        List<CategoryTreeNode> tree = service.categoryTree();

        assertThat(tree).extracting(CategoryTreeNode::slug).containsExactly("trang-suc", "thoi-trang");
        assertThat(tree.get(0).children()).extracting(CategoryTreeNode::slug).containsExactly("nhan");
    }

    @Test
    void listProducts_throwsNotFound_whenCategorySlugUnknown() {
        stubCategories();

        assertThatThrownBy(() -> service.listProducts(new ProductListQuery("khong-co", null, ProductSort.NEWEST, 0, 24)))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("messageKey", ProductErrorKeys.CATEGORY_NOT_FOUND);
        verify(productRepository, never()).pageNewest(anyBoolean(), anyCollection(), anyBoolean(), any(), any());
    }

    @Test
    void listProducts_queriesWholeSubtree_andBuildsCardFromCheapestVariant() {
        stubCategories();
        UUID productId = UUID.randomUUID();
        ProductRow row = new ProductRow(productId, "nhan-x", "Nhẫn X", Instant.now().minus(Duration.ofDays(2)));
        when(productRepository.pageNewest(eq(false), eq(List.of(ROOT, CHILD)), eq(true), any(), eq(PageRequest.of(0, 24))))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 24), 1));
        when(variantRepository.findActiveRowsByProductIdIn(List.of(productId))).thenReturn(List.of(
                new VariantRow(productId, UUID.randomUUID(), new BigDecimal("990000"), null, Map.of("color", "Mạ vàng")),
                new VariantRow(productId, UUID.randomUUID(), new BigDecimal("890000"), new BigDecimal("1090000"), Map.of("color", "Bạc")),
                new VariantRow(productId, UUID.randomUUID(), new BigDecimal("890000"), null, Map.of("color", "Bạc"))));
        when(imageRepository.findRowsByProductIdIn(List.of(productId))).thenReturn(List.of(
                new ImageRow(UUID.randomUUID(), productId, null, "/a.jpg", null, 0),
                new ImageRow(UUID.randomUUID(), productId, null, "/b.jpg", null, 1)));

        PageView<ProductCardView> page = service.listProducts(new ProductListQuery("trang-suc", null, ProductSort.NEWEST, 0, 24));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.hasNext()).isFalse();
        ProductCardView card = page.items().get(0);
        assertThat(card.price()).isEqualByComparingTo("890000");
        assertThat(card.compareAtPrice()).isEqualByComparingTo("1090000");
        assertThat(card.imageUrl()).isEqualTo("/a.jpg");
        assertThat(card.colors()).containsExactly("Mạ vàng", "Bạc");
        assertThat(card.label()).isEqualTo("NEW");
    }

    @Test
    void listProducts_withoutFilters_passesAllCategoriesFlag_andNoNewLabelForOldProduct() {
        stubCategories();
        UUID productId = UUID.randomUUID();
        when(productRepository.pagePriceAsc(eq(true), anyCollection(), eq(true), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new ProductRow(productId, "cu", "Cũ", Instant.now().minus(Duration.ofDays(90))))));
        when(variantRepository.findActiveRowsByProductIdIn(anyCollection())).thenReturn(List.of());
        when(imageRepository.findRowsByProductIdIn(anyCollection())).thenReturn(List.of());

        PageView<ProductCardView> page = service.listProducts(new ProductListQuery(null, null, ProductSort.PRICE_ASC, 0, 24));

        ProductCardView card = page.items().get(0);
        assertThat(card.price()).isNull();
        assertThat(card.label()).isNull();
        assertThat(card.colors()).isEmpty();
    }

    @Test
    void productDetail_throwsNotFound_whenSlugMissingOrNotActive() {
        when(productRepository.findBySlugAndStatus("x", ProductStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.productDetail("x"))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("messageKey", ProductErrorKeys.PRODUCT_NOT_FOUND);
    }
}
