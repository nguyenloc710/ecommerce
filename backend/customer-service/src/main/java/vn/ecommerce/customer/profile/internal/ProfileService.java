package vn.ecommerce.customer.profile.internal;

import java.util.UUID;
import vn.ecommerce.common.web.error.PageResponse;
import vn.ecommerce.customer.profile.CustomerProfileView;
import vn.ecommerce.customer.profile.ProfileApi;

/** Nghiệp vụ hồ sơ trong module; phần dùng chéo module nằm ở {@link ProfileApi}. */
public interface ProfileService extends ProfileApi {

    CustomerProfileView updateCurrent(ProfileUpdateCommand command);

    PageResponse<CustomerProfileView> searchForAdmin(String q, int page, int size);

    CustomerProfileView getForAdmin(UUID customerId);
}
