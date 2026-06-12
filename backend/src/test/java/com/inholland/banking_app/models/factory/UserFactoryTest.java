package com.inholland.banking_app.models.factory;

import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserFactoryTest {

    // --- createUser ---

    @Test
    @DisplayName("createUser() - should build a pending customer when role is CUSTOMER")
    void createUser_shouldBuildCustomer_whenRoleIsCustomer() {
        User user = UserFactory.createUser(customerRequest());

        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getCustomerProfile()).isNotNull();
        assertThat(user.getEmployeeProfile()).isNull();
    }

    @Test
    @DisplayName("createUser() - should build an employee when role is EMPLOYEE")
    void createUser_shouldBuildEmployee_whenRoleIsEmployee() {
        User user = UserFactory.createUser(employeeRequest());

        assertThat(user.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(user.getEmployeeProfile()).isNotNull();
        assertThat(user.getCustomerProfile()).isNull();
    }

    @Test
    @DisplayName("createUser() - should reject ADMIN, which is provisioned elsewhere")
    void createUser_shouldThrow_whenRoleIsAdmin() {
        UserRequest request = customerRequest();
        request.setRole(Role.ADMIN);

        assertThatThrownBy(() -> UserFactory.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADMIN");
    }

    // --- createPendingCustomer ---

    @Test
    @DisplayName("createPendingCustomer() - should set account credentials and mark the user active")
    void createPendingCustomer_shouldSetCoreUserFields() {
        User user = UserFactory.createPendingCustomer(customerRequest());

        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getUsername()).isEqualTo("john_doe");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    @DisplayName("createPendingCustomer() - should build a profile awaiting approval")
    void createPendingCustomer_shouldBuildPendingProfile() {
        User user = UserFactory.createPendingCustomer(customerRequest());
        CustomerProfile profile = user.getCustomerProfile();

        assertThat(profile.getFirstName()).isEqualTo("John");
        assertThat(profile.getLastName()).isEqualTo("Doe");
        assertThat(profile.getBsn()).isEqualTo("123456789");
        assertThat(profile.getPhoneNumber()).isEqualTo("+31612345678");
        assertThat(profile.getStatus()).isEqualTo(CustomerStatus.PENDING_APPROVAL);
        assertThat(profile.getRegisteredAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    @DisplayName("createPendingCustomer() - should link user and profile both ways")
    void createPendingCustomer_shouldLinkUserAndProfile() {
        User user = UserFactory.createPendingCustomer(customerRequest());

        assertThat(user.getCustomerProfile().getUser()).isSameAs(user);
    }

    // --- createEmployee ---

    @Test
    @DisplayName("createEmployee() - should set account credentials and mark the user active")
    void createEmployee_shouldSetCoreUserFields() {
        User user = UserFactory.createEmployee(employeeRequest());

        assertThat(user.getEmail()).isEqualTo("jane@bank.com");
        assertThat(user.getUsername()).isEqualTo("jane_doe");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    @DisplayName("createEmployee() - should build an enabled profile with an EMP- prefixed number")
    void createEmployee_shouldBuildEnabledProfile() {
        User user = UserFactory.createEmployee(employeeRequest());
        EmployeeProfile profile = user.getEmployeeProfile();

        assertThat(profile.getFirstName()).isEqualTo("Jane");
        assertThat(profile.getLastName()).isEqualTo("Doe");
        assertThat(profile.getEmployeeNumber()).isEqualTo("EMP-001");
        assertThat(profile.isEnabled()).isTrue();
        assertThat(profile.getCreatedAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    @DisplayName("createEmployee() - should link user and profile both ways")
    void createEmployee_shouldLinkUserAndProfile() {
        User user = UserFactory.createEmployee(employeeRequest());

        assertThat(user.getEmployeeProfile().getUser()).isSameAs(user);
    }

    // --- helpers ---

    private UserRequest customerRequest() {
        UserRequest request = new UserRequest();
        request.setRole(Role.CUSTOMER);
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setUsername("john_doe");
        request.setPassword("hashed-password");
        request.setBsn("123456789");
        request.setPhoneNumber("+31612345678");
        return request;
    }

    private UserRequest employeeRequest() {
        UserRequest request = new UserRequest();
        request.setRole(Role.EMPLOYEE);
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("jane@bank.com");
        request.setUsername("jane_doe");
        request.setPassword("hashed-password");
        request.setEmployeeNumber("001");
        return request;
    }
}
