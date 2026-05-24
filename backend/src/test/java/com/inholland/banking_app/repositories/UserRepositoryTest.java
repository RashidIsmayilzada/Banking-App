package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("test@bank.com");
        user.setUsername("testuser");
        user.setPasswordHash("hashed");
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Test
    @DisplayName("findByEmail() - should return user when email exists")
    void findByEmail_shouldReturnUser_whenEmailExists() {
        Optional<User> result = userRepository.findByEmail("test@bank.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@bank.com");
    }

    @Test
    @DisplayName("findByEmail() - should return empty when email not found")
    void findByEmail_shouldReturnEmpty_whenEmailNotFound() {
        Optional<User> result = userRepository.findByEmail("unknown@bank.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUsername() - should return user when username exists")
    void findByUsername_shouldReturnUser_whenUsernameExists() {
        Optional<User> result = userRepository.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findByUsername() - should return empty when username not found")
    void findByUsername_shouldReturnEmpty_whenUsernameNotFound() {
        Optional<User> result = userRepository.findByUsername("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail() - should return true when email exists")
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        boolean exists = userRepository.existsByEmail("test@bank.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail() - should return false when email not found")
    void existsByEmail_shouldReturnFalse_whenEmailNotFound() {
        boolean exists = userRepository.existsByEmail("nobody@bank.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByUsername() - should return true when username exists")
    void existsByUsername_shouldReturnTrue_whenUsernameExists() {
        boolean exists = userRepository.existsByUsername("testuser");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUsername() - should return false when username not found")
    void existsByUsername_shouldReturnFalse_whenUsernameNotFound() {
        boolean exists = userRepository.existsByUsername("nobody");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findAllWithNoAccounts() - should return users that have no accounts")
    void findAllWithNoAccounts_shouldReturnUsersWithNoAccounts() {
        Page<User> result = userRepository.findAllWithNoAccounts(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).anyMatch(u -> u.getEmail().equals("test@bank.com"));
    }
}
