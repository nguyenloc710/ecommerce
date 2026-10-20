package vn.ecommerce.customer.address.internal.division;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.customer.address.DivisionView;
import vn.ecommerce.customer.address.internal.AddressErrorKeys;
import vn.ecommerce.customer.address.internal.AddressMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class DivisionServiceImpl implements DivisionService {

    private final AdminDivisionRepository repository;
    private final AddressMapper mapper;

    @Override
    public List<DivisionView> provinces() {
        return repository.findByLevelOrderByNameAsc(AdminDivision.LEVEL_PROVINCE).stream().map(mapper::toView).toList();
    }

    @Override
    public List<DivisionView> wards(String provinceCode) {
        AdminDivision province = requireProvince(provinceCode);
        return repository.findByParentCodeOrderByNameAsc(province.getCode()).stream().map(mapper::toView).toList();
    }

    @Override
    public AdminDivision requireProvince(String provinceCode) {
        return repository.findByCodeAndLevel(provinceCode, AdminDivision.LEVEL_PROVINCE)
                .orElseThrow(() -> new NotFoundException(AddressErrorKeys.PROVINCE_NOT_FOUND, provinceCode));
    }

    @Override
    public AdminDivision requireWard(String provinceCode, String wardCode) {
        return repository.findByCodeAndParentCode(wardCode, provinceCode)
                .orElseThrow(() -> new BadRequestException(AddressErrorKeys.WARD_NOT_IN_PROVINCE, wardCode, provinceCode));
    }
}
