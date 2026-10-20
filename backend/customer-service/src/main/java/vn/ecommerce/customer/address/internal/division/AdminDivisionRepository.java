package vn.ecommerce.customer.address.internal.division;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminDivisionRepository extends JpaRepository<AdminDivision, String> {

    List<AdminDivision> findByLevelOrderByNameAsc(short level);

    List<AdminDivision> findByParentCodeOrderByNameAsc(String parentCode);

    Optional<AdminDivision> findByCodeAndLevel(String code, short level);

    Optional<AdminDivision> findByCodeAndParentCode(String code, String parentCode);
}
