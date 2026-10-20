package vn.ecommerce.catalog.product.internal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.ecommerce.catalog.product.ProductDetailView;
import vn.ecommerce.catalog.product.internal.domain.Brand;
import vn.ecommerce.catalog.product.internal.domain.Category;
import vn.ecommerce.catalog.product.internal.domain.ProductImage;
import vn.ecommerce.catalog.product.internal.domain.ProductVariant;

/** Chỉ đổi hình dạng entity → view; tính toán (giá thấp nhất, option, breadcrumb) nằm ở service. */
@Mapper
public interface ProductMapper {

    ProductDetailView.VariantView toVariantView(ProductVariant variant);

    @Mapping(target = "variantId", source = "variant.id")
    ProductDetailView.ImageView toImageView(ProductImage image);

    ProductDetailView.BrandRef toBrandRef(Brand brand);

    ProductDetailView.CategoryRef toCategoryRef(Category category);
}
