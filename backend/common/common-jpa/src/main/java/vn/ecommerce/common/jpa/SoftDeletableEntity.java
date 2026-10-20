package vn.ecommerce.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

/**
 * Entity xóa mềm: {@code deleted_at IS NULL} là còn hiệu lực. Entity con khai báo
 * {@code @SQLRestriction("deleted_at IS NULL")} để truy vấn đọc tự lọc; unique index phải là
 * partial index {@code WHERE deleted_at IS NULL}.
 */
@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends AuditableEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 64)
    private String deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Đánh dấu xóa; gọi trong transaction rồi {@code save}, không {@code DELETE}. */
    public void markDeleted(String by) {
        this.deletedAt = Instant.now();
        this.deletedBy = by;
    }
}
