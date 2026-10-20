package vn.ecommerce.customer.staffadmin.internal;

import java.util.List;
import java.util.UUID;
import vn.ecommerce.customer.staffadmin.internal.dto.LockCommand;
import vn.ecommerce.customer.staffadmin.internal.dto.StaffCreateCommand;
import vn.ecommerce.customer.staffadmin.internal.dto.StaffView;

/** Thao tác của nhân viên lên tài khoản khách và nhân viên khác; mọi thao tác đều ghi audit log. */
public interface StaffAdminService {

    /** Khóa ở Keycloak (enabled=false + đăng xuất mọi phiên) và đổi trạng thái hồ sơ sang LOCKED. */
    void lockCustomer(UUID customerId, LockCommand command);

    void unlockCustomer(UUID customerId);

    List<StaffView> listStaff(int page, int size);

    StaffView createStaff(StaffCreateCommand command);

    void setStaffEnabled(String staffId, boolean enabled);
}
