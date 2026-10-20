package vn.ecommerce.catalog.product.internal.admin;

import vn.ecommerce.common.jpa.AuditableEntity;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.CommonErrorKeys;
import vn.ecommerce.common.web.error.ConflictException;

/**
 * API cập nhật phải gửi {@code version} đã đọc; lệch với DB nghĩa là người khác đã sửa → 409,
 * không ghi đè mù (rule "Kiểm tra trước khi cập nhật").
 */
final class VersionCheck {

    private VersionCheck() {
    }

    static void require(AuditableEntity entity, Long requestedVersion) {
        if (requestedVersion == null) {
            throw new BadRequestException(CommonErrorKeys.VERSION_REQUIRED);
        }
        if (entity.getVersion() != requestedVersion) {
            throw new ConflictException(CommonErrorKeys.CONCURRENT_MODIFICATION);
        }
    }
}
