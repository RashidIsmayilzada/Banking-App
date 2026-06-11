package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User customer;
    private User employee;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setEmail("john@example.com");
        customer.setUsername("john_doe");
        customer.setPasswordHash("hashed");
        customer.setRole(Role.CUSTOMER);
        customer.setActive(true);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        CustomerProfile customerProfile = new CustomerProfile();
        customerProfile.setFirstName("John");
        customerProfile.setLastName("Doe");
        customerProfile.setBsn("123456789");
        customerProfile.setPhoneNumber("+31612345678");
        customerProfile.setStatus(CustomerStatus.PENDING_APPROVAL);
        customerProfile.setRegisteredAt(LocalDateTime.now());
        customerProfile.setUser(customer);
        customer.setCustomerProfile(customerProfile);

        customer = userRepository.save(customer);

        employee = new User();
        employee.setEmail("jane@example.com");
        employee.setUsername("jane_smith");
        employee.setPasswordHash("hashed");
        employee.setRole(Role.EMPLOYEE);
        employee.setActive(true);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());

        EmployeeProfile employeeProfile = new EmployeeProfile();
        employeeProfile.setFirstName("Jane");
        employeeProfile.setLastName("Smith");
        employeeProfile.setEmployeeNumber("EMP-001");
        employeeProfile.setEnabled(true);
        employeeProfile.setCreatedAt(LocalDateTime.now());
        employeeProfile.setUser(employee);
        employee.setEmployeeProfile(employeeProfile);

        employee = userRepository.save(employee);
    }

    // --- findByEmail ---

    @Test
    @DisplayName("findByEmail() - should return user when email exists")
    void findByEmail_shouldReturnUser_whenEmailExists() {
        Optional<User> result = userRepository.findByEmail("john@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("findByEmail() - should return empty when email does not exist")
    void findByEmail_shouldReturnEmpty_whenEmailNotFound() {
        Optional<User> result = userRepository.findByEmail("unknown@example.com");

        assertThat(result).isEmpty();
    }

    // --- findByUsername ---

    @Test
    @DisplayName("findByUsername() - should return user when username exists")
    void findByUsername_shouldReturnUser_whenUsernameExists() {
        Optional<User> result = userRepository.findByUsername("john_doe");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("findByUsername() - should return empty when username does not exist")
    void findByUsername_shouldReturnEmpty_whenUsernameNotFound() {
        Optional<User> result = userRepository.findByUsername("unknown_user");

        assertThat(result).isEmpty();
    }

    // --- existsByEmail ---

    @Test
    @DisplayName("existsByEmail() - should return true when email exists")
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail() - should return false when email does not exist")
    void existsByEmail_shouldReturnFalse_whenEmailNotFound() {
        assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
    }

    // --- existsByUsername ---

    @Test
    @DisplayName("existsByUsername() - should return true when username exists")
    void existsByUsername_shouldReturnTrue_whenUsernameExists() {
        assertThat(userRepository.existsByUsername("john_doe")).isTrue();
    }

    @Test
    @DisplayName("existsByUsername() - should return false when username does not exist")
    void existsByUsername_shouldReturnFalse_whenUsernameNotFound() {
        assertThat(userRepository.existsByUsername("ghost_user")).isFalse();
    }

    // --- findAll with Specification ---

    @Test
    @DisplayName("findAll() - should return all users when no filter applied")
    void findAll_shouldReturnAllUsers_whenNoFilter() {
        Page<User> result = userRepository.findAll(Specification.unrestricted(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findAll() - should return only customers when role filter is CUSTOMER")
    void findAll_shouldReturnOnlyCustomers_whenRoleFilterApplied() {
        Specification<User> roleSpec = (root, query, cb) -> cb.equal(root.get("role"), Role.CUSTOMER);

        Page<User> result = userRepository.findAll(roleSpec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("findAll() - should return only employees when role filter is EMPLOYEE")
    void findAll_shouldReturnOnlyEmployees_whenRoleFilterApplied() {
        Specification<User> roleSpec = (root, query, cb) -> cb.equal(root.get("role"), Role.EMPLOYEE);

        Page<User> result = userRepository.findAll(roleSpec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRole()).isEqualTo(Role.EMPLOYEE);
    }

    @Test
    @DisplayName("findAll() - should respect pagination and return correct page")
    void findAll_shouldRespectPagination() {
        Page<User> firstPage = userRepository.findAll(Specification.unrestricted(), PageRequest.of(0, 1));
        Page<User> secondPage = userRepository.findAll(Specification.unrestricted(), PageRequest.of(1, 1));

        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent().get(0).getId())
                .isNotEqualTo(secondPage.getContent().get(0).getId());
    }

    // --- @EntityGraph: eager profile loading ---

    @Test
    @DisplayName("findAll() - should eagerly load profiles via @EntityGraph to prevent N+1")
    void findAll_shouldEagerlyLoadProfiles_viaEntityGraph() {
        Page<User> page = userRepository.findAll(Specification.unrestricted(), PageRequest.of(0, 10));

        // Clearing the persistence context detaches all entities.
        // Accessing an uninitialised lazy proxy on a detached entity throws LazyInitializationException.
        // If the EntityGraph loaded the profiles eagerly, they are already initialised and accessible.
        entityManager.clear();

        for (User user : page.getContent()) {
            if (user.getRole() == Role.CUSTOMER) {
                assertThat(user.getCustomerProfile()).isNotNull();
                assertThat(user.getCustomerProfile().getFirstName()).isEqualTo("John");
            } else if (user.getRole() == Role.EMPLOYEE) {
                assertThat(user.getEmployeeProfile()).isNotNull();
                assertThat(user.getEmployeeProfile().getFirstName()).isEqualTo("Jane");
            }
        }
    }
}
