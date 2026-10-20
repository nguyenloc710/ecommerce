package vn.ecommerce.catalog.product.internal.admin;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.catalog.product.PageView;
import vn.ecommerce.catalog.product.internal.Slugs;
import vn.ecommerce.catalog.product.internal.admin.dto.BrandAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.BrandUpsertCommand;
import vn.ecommerce.catalog.product.internal.domain.Brand;
import vn.ecommerce.catalog.product.internal.repo.BrandRepository;
import vn.ecommerce.catalog.product.internal.repo.ProductRepository;
import vn.ecommerce.common.security.CurrentUser;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.common.web.error.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class BrandAdminServiceImpl implements BrandAdminService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final AdminMapper mapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public PageView<BrandAdminView> list(int page, int size) {
        Page<Brand> result = brandRepository.findAll(PageRequest.of(page, size, Sort.by("name")));
        return new PageView<>(result.getContent().stream().map(mapper::toBrandView).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    public BrandAdminView create(BrandUpsertCommand command) {
        String slug = Slugs.resolve(command.slug(), command.name());
        if (brandRepository.existsBySlug(slug)) {
            throw new ConflictException(AdminErrorKeys.BRAND_SLUG_TAKEN, slug);
        }
        Brand brand = new Brand(command.name(), slug);
        brand.setLogoUrl(command.logoUrl());
        brand.setDescription(command.description());
        Brand saved = brandRepository.save(brand);
        log.info("Tạo thương hiệu {} ({})", saved.getSlug(), saved.getId());
        return mapper.toBrandView(saved);
    }

    @Override
    public BrandAdminView update(UUID id, BrandUpsertCommand command) {
        Brand brand = load(id);
        VersionCheck.require(brand, command.version());
        String slug = Slugs.resolve(command.slug(), command.name());
        if (brandRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException(AdminErrorKeys.BRAND_SLUG_TAKEN, slug);
        }
        brand.setName(command.name());
        brand.setSlug(slug);
        brand.setLogoUrl(command.logoUrl());
        brand.setDescription(command.description());
        return mapper.toBrandView(brandRepository.save(brand));
    }

    @Override
    public void delete(UUID id) {
        Brand brand = load(id);
        long products = productRepository.countByBrandId(id);
        if (products > 0) {
            throw new ConflictException(AdminErrorKeys.BRAND_IN_USE, products);
        }
        brand.markDeleted(currentUser.requireId());
        brandRepository.save(brand);
        log.info("Xóa mềm thương hiệu {} ({})", brand.getSlug(), id);
    }

    private Brand load(UUID id) {
        return brandRepository.findById(id).orElseThrow(() -> new NotFoundException(AdminErrorKeys.BRAND_NOT_FOUND_ID));
    }
}
