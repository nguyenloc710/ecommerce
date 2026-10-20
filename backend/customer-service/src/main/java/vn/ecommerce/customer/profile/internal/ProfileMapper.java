package vn.ecommerce.customer.profile.internal;

import org.mapstruct.Mapper;
import vn.ecommerce.customer.profile.CustomerProfileView;

@Mapper
public interface ProfileMapper {

    CustomerProfileView toView(CustomerProfile profile);
}
