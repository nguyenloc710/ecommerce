package vn.ecommerce.catalog.product.internal.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryUpsertCommand;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.repo.CategoryRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductRepository;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;

@ExtendWith(MockitoExtension.class)
class CategoryAdminServiceImplTest {

    @Mock CategoryRepository categoryRepository;
    @Mock ProductRepository productRepository;
    @Mock CurrentUser currentUser;

    private CategoryAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CategoryAdminServiceImpl(categoryRepository, productRepository, Mappers.getMapper(AdminMapper.class), currentUser);
    }

    @Test
    void create_generatesSlugFromVietnameseName_andSaves() {
        when(categoryRepository.existsBySlug("vay-dam-du-tiec")).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryAdminView view = service.create(new CategoryUpsertCommand("Váy đầm dự tiệc", null, null, null, null, 2, null, null));

        assertThat(view.slug()).isEqualTo("vay-dam-du-tiec");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.sortOrder()).isEqualTo(2);
    }

    @Test
    void create_rejectsDuplicateSlug_withoutSaving() {
        when(categoryRepository.existsBySlug("nhan")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryUpsertCommand("Nhẫn", null, null, null, null, null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", AdminErrorKeys.CATEGORY_SLUG_TAKEN);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_requiresVersion_andRejectsStaleVersion() {
        Category existing = new Category("Nhẫn", "nhan", null);
        existing.setVersion(3);
        UUID id = existing.getId();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(id, new CategoryUpsertCommand("Nhẫn", null, null, null, null, null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("messageKey", CommonErrorKeys.VERSION_REQUIRED);
        assertThatThrownBy(() -> service.update(id, new CategoryUpsertCommand("Nhẫn", null, null, null, null, null, null, 2L)))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", CommonErrorKeys.CONCURRENT_MODIFICATION);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_rejectsParentThatIsDescendant() {
        Category root = new Category("Trang sức", "trang-suc", null);
        Category child = new Category("Nhẫn", "nhan", root);
        when(categoryRepository.findById(root.getId())).thenReturn(Optional.of(root));
        when(categoryRepository.existsBySlugAndIdNot("trang-suc", root.getId())).thenReturn(false);
        when(categoryRepository.findById(child.getId())).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service.update(root.getId(),
                new CategoryUpsertCommand("Trang sức", null, child.getId(), null, null, null, null, 0L)))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("messageKey", AdminErrorKeys.CATEGORY_PARENT_CYCLE);
    }

    @Test
    void delete_rejectsWhenChildrenOrProductsRemain() {
        Category category = new Category("Nhẫn", "nhan", null);
        UUID id = category.getId();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", AdminErrorKeys.CATEGORY_HAS_CHILDREN);

        when(categoryRepository.existsByParentId(id)).thenReturn(false);
        when(productRepository.countByCategoryId(id)).thenReturn(4L);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("messageKey", AdminErrorKeys.CATEGORY_HAS_PRODUCTS);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_softDeletesWithCurrentUser() {
        Category category = new Category("Nhẫn", "nhan", null);
        UUID id = category.getId();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentId(id)).thenReturn(false);
        when(productRepository.countByCategoryId(id)).thenReturn(0L);
        when(currentUser.requireId()).thenReturn("staff-1");

        service.delete(id);

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().isDeleted()).isTrue();
        assertThat(saved.getValue().getDeletedBy()).isEqualTo("staff-1");
    }
}
