package vn.ecommerce.customer.address.internal;

import java.util.List;
import java.util.UUID;
import vn.ecommerce.customer.address.AddressView;

/** Sổ địa chỉ của khách đang đăng nhập; mọi thao tác đều kiểm tra quyền sở hữu. */
public interface AddressService {

    List<AddressView> listMine();

    AddressView create(AddressUpsertCommand command);

    AddressView update(UUID addressId, AddressUpsertCommand command);

    AddressView setDefault(UUID addressId);

    void delete(UUID addressId);

    /** Cho order-service đọc địa chỉ của khách khi checkout (không tin dữ liệu client gửi lên). */
    AddressView getForCustomer(UUID customerId, UUID addressId);
}
