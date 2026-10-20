package vn.ecommerce.customer.address.internal.division;

import java.util.List;
import vn.ecommerce.customer.address.DivisionView;

/** Tra cứu tỉnh/thành và phường/xã cho form địa chỉ và kiểm tra dữ liệu đầu vào. */
public interface DivisionService {

    List<DivisionView> provinces();

    /** Phường/xã của một tỉnh; tỉnh không tồn tại thì ném NotFound. */
    List<DivisionView> wards(String provinceCode);

    AdminDivision requireProvince(String provinceCode);

    /** Phường phải thuộc đúng tỉnh gửi kèm, nếu không ném BadRequest. */
    AdminDivision requireWard(String provinceCode, String wardCode);
}
