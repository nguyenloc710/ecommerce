package vn.ecommerce.catalog.product.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import vn.ecommerce.common.jpa.SoftDeletableEntity;

/**
 * Danh mục dạng cây (Thời trang > Áo > Áo thun). {@code attributeTemplate} mô tả thuộc tính và option
 * của sản phẩm trong danh mục (ví dụ trang sức: chất liệu, size nhẫn, đá chủ).
 */
@Getter
@Setter
@Entity
@Table(name = "category")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 140)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attribute_template", nullable = false)
    private Map<String, Object> attributeTemplate = new LinkedHashMap<>();

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    public Category(String name, String slug, Category parent) {
        this.name = name;
        this.slug = slug;
        this.parent = parent;
    }
}
