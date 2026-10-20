package vn.ecommerce.customer.address.internal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.ecommerce.customer.address.AddressView;
import vn.ecommerce.customer.address.DivisionView;
import vn.ecommerce.customer.address.internal.division.AdminDivision;

@Mapper
public interface AddressMapper {

    /** Lombok sinh {@code isDefault()} nên MapStruct thấy property tên {@code default}. */
    @Mapping(target = "isDefault", source = "default")
    AddressView toView(Address address);

    DivisionView toView(AdminDivision division);
}
