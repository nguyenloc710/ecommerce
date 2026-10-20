package vn.ecommerce.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.ecommerce.common.web.error.NotFoundException;
import vn.ecommerce.customer.address.AddressView;
import vn.ecommerce.customer.address.DivisionView;
import vn.ecommerce.customer.address.internal.AddressService;
import vn.ecommerce.customer.address.internal.AddressUpsertCommand;
import vn.ecommerce.customer.address.internal.division.DivisionService;
import vn.ecommerce.customer.profile.CustomerProfileView;
import vn.ecommerce.customer.profile.internal.ProfileService;
import vn.ecommerce.customer.wishlist.WishlistItemView;
import vn.ecommerce.customer.wishlist.internal.WishlistAddCommand;
import vn.ecommerce.customer.wishlist.internal.WishlistService;

/**
 * Cả ứng dụng trên PostgreSQL thật: Flyway chạy migration + nạp 34 tỉnh / 3.321 phường; luồng khách đăng nhập lần đầu
 * → tạo hồ sơ JIT → thêm địa chỉ (snapshot tên tỉnh/phường, mặc định duy nhất) → wishlist idempotent → xóa mềm địa chỉ.
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/certs",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        // Giữ externalization bật để kiểm tra event customer.created được ghi outbox (gửi RabbitMQ thất bại chỉ log lỗi)
        "spring.rabbitmq.connection-timeout=2s"
})
class CustomerServiceDbTest {

    private static final String HA_NOI = "01";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired DivisionService divisionService;
    @Autowired ProfileService profileService;
    @Autowired AddressService addressService;
    @Autowired WishlistService wishlistService;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void referenceData_isLoadedByFlyway() {
        List<DivisionView> provinces = divisionService.provinces();
        assertThat(provinces).hasSize(34);
        assertThat(provinces).extracting(DivisionView::code).contains(HA_NOI, "79");

        List<DivisionView> wards = divisionService.wards(HA_NOI);
        assertThat(wards).isNotEmpty().allSatisfy(w -> assertThat(w.parentCode()).isEqualTo(HA_NOI));
        assertThat(jdbc.queryForObject("select count(*) from admin_division where level = 2", Long.class)).isEqualTo(3321L);
    }

    @Test
    void firstLogin_createsProfile_thenAddressBookAndWishlistWork() {
        UUID sub = UUID.randomUUID();
        signInAs(sub, "lan@example.com", "Lan Nguyễn");

        CustomerProfileView profile = profileService.ensureCurrentProfile();
        assertThat(profile.id()).isEqualTo(sub);
        assertThat(profile.fullName()).isEqualTo("Lan Nguyễn");
        assertThat(jdbc.queryForObject("select created_by from customer_profile where id = ?", String.class, sub))
                .isEqualTo(sub.toString());
        // Event customer.created được ghi vào outbox trong cùng transaction
        assertThat(jdbc.queryForObject("select count(*) from event_publication", Long.class)).isPositive();

        String ward = divisionService.wards(HA_NOI).getFirst().code();
        AddressView first = addressService.create(new AddressUpsertCommand("Lan", "0912345678", HA_NOI, ward, "1 Hàng Bài", null, false, null));
        AddressView second = addressService.create(new AddressUpsertCommand("Lan", "0912345678", HA_NOI, ward, "2 Tràng Tiền", null, true, null));

        assertThat(first.isDefault()).isTrue();
        assertThat(first.provinceName()).isEqualTo("Thành phố Hà Nội");
        List<AddressView> mine = addressService.listMine();
        assertThat(mine).hasSize(2);
        assertThat(mine.getFirst().id()).as("địa chỉ mặc định mới đứng đầu").isEqualTo(second.id());
        assertThat(mine.stream().filter(AddressView::isDefault).count()).isEqualTo(1);

        addressService.delete(second.id());
        assertThat(addressService.listMine()).extracting(AddressView::id).containsExactly(first.id());
        assertThat(jdbc.queryForObject("select deleted_at is not null from address where id = ?", Boolean.class, second.id())).isTrue();
        assertThatThrownBy(() -> addressService.getForCustomer(sub, second.id())).isInstanceOf(NotFoundException.class);

        UUID sku = UUID.randomUUID();
        WishlistItemView added = wishlistService.add(sku, new WishlistAddCommand("nhan-bac-moissanite"));
        WishlistItemView again = wishlistService.add(sku, new WishlistAddCommand("nhan-bac-moissanite"));
        assertThat(again.id()).isEqualTo(added.id());
        wishlistService.remove(sku);
        assertThat(wishlistService.listMine()).isEmpty();
    }

    private static void signInAs(UUID sub, String email, String name) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none")
                .subject(sub.toString()).claim("email", email).claim("name", name)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }
}
