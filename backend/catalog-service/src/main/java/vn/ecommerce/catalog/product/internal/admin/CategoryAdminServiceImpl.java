package vn.ecommerce.catalog.product.internal.admin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.product.internal.Slugs;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryUpsertCommand;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.CategoryStatus;
import vn.ecommerce.catalog.product.internal.repo.CategoryRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductRepository;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class CategoryAdminServiceImpl implements CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AdminMapper mapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryAdminView> list() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream().map(mapper::toCategoryView).toList();
    }

    @Override
    public CategoryAdminView create(CategoryUpsertCommand command) {
        String slug = Slugs.resolve(command.slug(), command.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException(AdminErrorKeys.CATEGORY_SLUG_TAKEN, slug);
        }
        Category parent = resolveParent(command.parentId(), null);
        Category category = new Category(command.name(), slug, parent);
        apply(category, command);
        Category saved = categoryRepository.save(category);
        log.info("Tạo danh mục {} ({})", saved.getSlug(), saved.getId());
        return mapper.toCategoryView(saved);
    }

    @Override
    public CategoryAdminView update(UUID id, CategoryUpsertCommand command) {
        Category category = load(id);
        VersionCheck.require(category, command.version());
        String slug = Slugs.resolve(command.slug(), command.name());
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException(AdminErrorKeys.CATEGORY_SLUG_TAKEN, slug);
        }
        category.setParent(resolveParent(command.parentId(), category));
        category.setName(command.name());
        category.setSlug(slug);
        apply(category, command);
        return mapper.toCategoryView(categoryRepository.save(category));
    }

    @Override
    public void delete(UUID id) {
        Category category = load(id);
        if (categoryRepository.existsByParentId(id)) {
            throw new ConflictException(AdminErrorKeys.CATEGORY_HAS_CHILDREN);
        }
        long products = productRepository.countByCategoryId(id);
        if (products > 0) {
            throw new ConflictException(AdminErrorKeys.CATEGORY_HAS_PRODUCTS, products);
        }
        category.markDeleted(currentUser.requireId());
        categoryRepository.save(category);
        log.info("Xóa mềm danh mục {} ({})", category.getSlug(), id);
    }

    private Category load(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(AdminErrorKeys.CATEGORY_NOT_FOUND_ID));
    }

    private static void apply(Category category, CategoryUpsertCommand command) {
        category.setDescription(command.description());
        category.setAttributeTemplate(command.attributeTemplate() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(command.attributeTemplate()));
        category.setSortOrder(command.sortOrder() == null ? 0 : command.sortOrder());
        category.setStatus(command.status() == null ? CategoryStatus.ACTIVE : CategoryStatus.valueOf(command.status()));
    }

    /** Cha phải tồn tại và không được là chính nó hay con cháu của nó (tránh vòng). */
    private Category resolveParent(UUID parentId, Category self) {
        if (parentId == null) {
            return null;
        }
        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new BadRequestException(AdminErrorKeys.CATEGORY_PARENT_NOT_FOUND));
        if (self != null) {
            for (Category c = parent; c != null; c = c.getParent()) {
                if (Objects.equals(c.getId(), self.getId())) {
                    throw new BadRequestException(AdminErrorKeys.CATEGORY_PARENT_CYCLE);
                }
            }
        }
        return parent;
    }
}
