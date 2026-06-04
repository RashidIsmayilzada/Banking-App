package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class CustomerProfileRepositoryTest {

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("customer@bank.com");
        user.setUsername("customeruser");
        user.setPasswordHash("hashed");
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setFirstName("Jane");
        profile.setLastName("Doe");
        profile.setBsn("123456789");
        profile.setPhoneNumber("0612345678");
        profile.setStatus(CustomerStatus.PENDING_APPROVAL);
        profile.setRegisteredAt(LocalDateTime.now());
        customerProfileRepository.save(profile);
    }

    @Test
    @DisplayName("existsByBsn() - should return true when BSN exists")
    void existsByBsn_shouldReturnTrue_whenBsnExists() {
        boolean exists = customerProfileRepository.existsByBsn("123456789");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByBsn() - should return false when BSN not found")
    void existsByBsn_shouldReturnFalse_whenBsnNotFound() {
        boolean exists = customerProfileRepository.existsByBsn("999999999");

        assertThat(exists).isFalse();
    }
}
