package vn.ecommerce.catalog.product.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import vn.ecommerce.common.jpa.SoftDeletableEntity;

@Getter
@Setter
@Entity
@Table(name = "brand")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends SoftDeletableEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 140)
    private String slug;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(columnDefinition = "text")
    private String description;

    public Brand(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
}
