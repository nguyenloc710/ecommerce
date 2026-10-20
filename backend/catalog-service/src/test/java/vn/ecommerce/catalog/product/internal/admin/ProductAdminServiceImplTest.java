package vn.ecommerce.catalog.product.internal.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.catalog.product.internal.admin.dto.ImageItemCommand;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductUpsertCommand;
import vn.ecommerce.catalog.product.internal.admin.dto.VariantUpsertCommand;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.Product;
import vn.ecommerce.catalog.product.internal.domain.ProductImage;
import vn.ecommerce.catalog.product.internal.domain.ProductStatus;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;
import vn.ecommerce.catalog.product.internal.repo.BrandRepository;
import vn.ecommerce.catalog.product.internal.repo.CategoryRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductImageRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductVariantRepository;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;

@ExtendWith(MockitoExtension.class)
class ProductAdminServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock ProductImageRepository imageRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock BrandRepository brandRepository;
    @Mock CurrentUser currentUser;

    private ProductAdminServiceImpl service;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new ProductAdminServiceImpl(productRepository, variantRepository, imageRepository, categoryRepository,
                brandRepository, Mappers.getMapper(AdminMapper.class), currentUser);
        category = new Category("Nhẫn", "nhan", null);
    }

    private ProductUpsertCommand command(String status, Long version) {
        return new ProductUpsertCommand(category.getId(), null, "Nhẫn bạc", null, "mô tả", Map.of("Chất liệu", "Bạc 925"),
                "FIXED", status, null, null, version);
    }

    @Test
    void create_withActiveStatus_setsPublishedAt() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("nhan-bac")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantRepository.findByProductIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(any())).thenReturn(List.of());

        ProductAdminView view = service.create(command("ACTIVE", null));

        assertThat(view.slug()).isEqualTo("nhan-bac");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.publishedAt()).isNotNull();
        assertThat(view.categoryId()).isEqualTo(category.getId());
    }

    @Test
    void create_asDraft_leavesPublishedAtNull_andRejectsDuplicateSlug() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("nhan-bac")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantRepository.findByProductIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(any())).thenReturn(List.of());

        assertThat(service.create(command("DRAFT", null)).publishedAt()).isNull();

        when(productRepository.existsBySlug("nhan-bac")).thenReturn(true);
        assertThatThrownBy(() -> service.create(command("DRAFT", null)))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", AdminErrorKeys.PRODUCT_SLUG_TAKEN);
    }

    @Test
    void update_keepsOriginalPublishedAt_whenAlreadyPublished() {
        Product product = new Product(category, "Nhẫn bạc", "nhan-bac");
        product.setStatus(ProductStatus.ACTIVE);
        Instant firstPublished = Instant.parse("2026-01-01T00:00:00Z");
        product.setPublishedAt(firstPublished);
        when(productRepository.findWithRelationsById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.existsBySlugAndIdNot("nhan-bac", product.getId())).thenReturn(false);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantRepository.findByProductIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(any())).thenReturn(List.of());

        ProductAdminView view = service.update(product.getId(), command("HIDDEN", 0L));

        assertThat(view.status()).isEqualTo("HIDDEN");
        assertThat(view.publishedAt()).isEqualTo(firstPublished);
    }

    @Test
    void addVariant_rejectsDuplicateSku_withoutSaving() {
        Product product = new Product(category, "Nhẫn bạc", "nhan-bac");
        when(productRepository.findWithRelationsById(product.getId())).thenReturn(Optional.of(product));
        when(variantRepository.existsBySkuCode("RNG-1")).thenReturn(true);

        assertThatThrownBy(() -> service.addVariant(product.getId(),
                new VariantUpsertCommand("RNG-1", Map.of("size", "6"), new BigDecimal("890000"), null, null, null, null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", AdminErrorKeys.VARIANT_SKU_TAKEN);
        verify(variantRepository, never()).save(any());
    }

    @Test
    void delete_softDeletesProductAndAllVariants() {
        Product product = new Product(category, "Nhẫn bạc", "nhan-bac");
        ProductVariant v1 = new ProductVariant(product, "RNG-1", BigDecimal.ONE);
        ProductVariant v2 = new ProductVariant(product, "RNG-2", BigDecimal.ONE);
        when(productRepository.findWithRelationsById(product.getId())).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySortOrderAsc(product.getId())).thenReturn(List.of(v1, v2));
        when(currentUser.requireId()).thenReturn("staff-1");

        service.delete(product.getId());

        assertThat(product.isDeleted()).isTrue();
        assertThat(v1.isDeleted()).isTrue();
        assertThat(v2.getDeletedBy()).isEqualTo("staff-1");
        verify(variantRepository).saveAll(List.of(v1, v2));
    }

    @Test
    void replaceImages_rejectsVariantOfOtherProduct_beforeAnyWrite() {
        Product product = new Product(category, "Nhẫn bạc", "nhan-bac");
        when(productRepository.findWithRelationsById(product.getId())).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySortOrderAsc(product.getId())).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(product.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.replaceImages(product.getId(),
                List.of(new ImageItemCommand(null, UUID.randomUUID(), "/x.jpg", null))))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("messageKey", AdminErrorKeys.IMAGE_VARIANT_MISMATCH);
        verify(imageRepository, never()).saveAll(anyList());
        verify(imageRepository, never()).deleteAllInBatch(anyList());
    }

    @Test
    void replaceImages_reordersKeptImages_andDeletesMissingOnes() {
        Product product = new Product(category, "Nhẫn bạc", "nhan-bac");
        ProductImage keep = new ProductImage(product, "/a.jpg", 0);
        ProductImage drop = new ProductImage(product, "/b.jpg", 1);
        when(productRepository.findWithRelationsById(product.getId())).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySortOrderAsc(product.getId())).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderBySortOrderAsc(product.getId())).thenReturn(List.of(keep, drop), List.of());

        service.replaceImages(product.getId(), List.of(
                new ImageItemCommand(null, null, "/new.jpg", "mới"),
                new ImageItemCommand(keep.getId(), null, "/a.jpg", "giữ")));

        ArgumentCaptor<List<ProductImage>> saved = ArgumentCaptor.forClass(List.class);
        verify(imageRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ProductImage::getUrl).containsExactly("/new.jpg", "/a.jpg");
        assertThat(saved.getValue()).extracting(ProductImage::getSortOrder).containsExactly(0, 1);
        verify(imageRepository).deleteAllInBatch(List.of(drop));
    }
}
