package vn.ecommerce.catalog.product.internal.admin;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.ecommerce.catalog.product.internal.admin.dto.BrandAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.CategoryAdminView;
import vn.ecommerce.catalog.product.internal.admin.dto.ProductAdminView;
import vn.ecommerce.catalog.product.internal.domain.Brand;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.Product;
import vn.ecommerce.catalog.product.internal.domain.ProductImage;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;

@Mapper
public interface AdminMapper {

    @Mapping(target = "parentId", source = "parent.id")
    CategoryAdminView toCategoryView(Category category);

    BrandAdminView toBrandView(Brand brand);

    @Mapping(target = "categoryId", source = "product.category.id")
    @Mapping(target = "brandId", source = "product.brand.id")
    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "version", source = "product.version")
    @Mapping(target = "updatedAt", source = "product.updatedAt")
    @Mapping(target = "updatedBy", source = "product.updatedBy")
    ProductAdminView toProductView(Product product, List<ProductAdminView.VariantAdminView> variants,
                                   List<ProductAdminView.ImageAdminView> images);

    ProductAdminView.VariantAdminView toVariantView(ProductVariant variant);

    @Mapping(target = "variantId", source = "variant.id")
    ProductAdminView.ImageAdminView toImageView(ProductImage image);
}
