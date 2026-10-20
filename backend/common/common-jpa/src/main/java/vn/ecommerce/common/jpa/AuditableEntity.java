package vn.ecommerce.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entity cơ sở cho mọi bảng nghiệp vụ: id UUID sinh ở ứng dụng, audit cột do Spring Data điền
 * (user lấy từ JWT qua {@link CurrentAuditorProvider}), {@code version} cho Optimistic Locking.
 * <p>
 * Không set tay các cột audit từ request.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 64)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 64)
    private String updatedBy;

    @Version
    @Setter(AccessLevel.PUBLIC)
    @Column(nullable = false)
    private long version;

    /** Dùng khi id là khóa tự nhiên có sẵn (ví dụ {@code sub} của Keycloak làm id hồ sơ khách). Gọi trong constructor. */
    protected void assignId(UUID id) {
        this.id = id;
    }
}
