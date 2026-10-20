package vn.ecommerce.customer.address.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(UUID customerId);

    Optional<Address> findByIdAndCustomerId(UUID id, UUID customerId);

    long countByCustomerId(UUID customerId);

    /** Bỏ cờ mặc định của mọi địa chỉ khác trước khi đặt địa chỉ mới làm mặc định (unique partial index). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Address a set a.isDefault = false where a.customerId = :customerId and a.isDefault = true and a.id <> :keepId and a.deletedAt is null")
    int clearDefaultExcept(@Param("customerId") UUID customerId, @Param("keepId") UUID keepId);
}
