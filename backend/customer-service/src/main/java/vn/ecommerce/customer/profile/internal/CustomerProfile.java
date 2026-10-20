package vn.ecommerce.customer.profile.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ecommerce.common.jpa.AuditableEntity;
import vn.ecommerce.customer.profile.CustomerStatus;

/** Hồ sơ khách; id chính là {@code sub} của Keycloak nên không sinh ngẫu nhiên. */
@Getter
@Setter
@Entity
@Table(name = "customer_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerProfile extends AuditableEntity {

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    public CustomerProfile(UUID keycloakSub, String email, String fullName) {
        assignId(keycloakSub);
        this.email = email;
        this.fullName = fullName;
    }

    public enum Gender {
        FEMALE,
        MALE,
        OTHER
    }
}
