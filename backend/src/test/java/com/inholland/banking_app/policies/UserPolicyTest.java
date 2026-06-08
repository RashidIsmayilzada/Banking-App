package com.inholland.banking_app.policies;

import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPolicyTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerProfileRepository customerProfileRepository;
    @Mock private EmployeeProfileRepository employeeProfileRepository;

    @InjectMocks private UserPolicy userPolicy;

    // --- assertUniqueEmail ---

    @Test
    @DisplayName("assertUniqueEmail() - should not throw when email is not taken")
    void assertUniqueEmail_shouldNotThrow_whenEmailIsAvailable() {
        when(userRepository.existsByEmail("new@bank.com")).thenReturn(false);

        assertThatCode(() -> userPolicy.assertUniqueEmail("new@bank.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertUniqueEmail() - should throw DuplicateResourceException when email already exists")
    void assertUniqueEmail_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("taken@bank.com")).thenReturn(true);

        assertThatThrownBy(() -> userPolicy.assertUniqueEmail("taken@bank.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
    }

    // --- assertUniqueUsername ---

    @Test
    @DisplayName("assertUniqueUsername() - should not throw when username is not taken")
    void assertUniqueUsername_shouldNotThrow_whenUsernameIsAvailable() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        assertThatCode(() -> userPolicy.assertUniqueUsername("newuser")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertUniqueUsername() - should throw DuplicateResourceException when username already exists")
    void assertUniqueUsername_shouldThrow_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("takenuser")).thenReturn(true);

        assertThatThrownBy(() -> userPolicy.assertUniqueUsername("takenuser"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");
    }

    // --- assertUniqueBsn ---

    @Test
    @DisplayName("assertUniqueBsn() - should not throw when BSN is not taken")
    void assertUniqueBsn_shouldNotThrow_whenBsnIsAvailable() {
        when(customerProfileRepository.existsByBsn("123456789")).thenReturn(false);

        assertThatCode(() -> userPolicy.assertUniqueBsn("123456789")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertUniqueBsn() - should throw DuplicateResourceException when BSN already exists")
    void assertUniqueBsn_shouldThrow_whenBsnAlreadyExists() {
        when(customerProfileRepository.existsByBsn("123456789")).thenReturn(true);

        assertThatThrownBy(() -> userPolicy.assertUniqueBsn("123456789"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Bsn");
    }

    // --- assertUniqueEmployeeNumber ---

    @Test
    @DisplayName("assertUniqueEmployeeNumber() - should not throw when employee number is not taken")
    void assertUniqueEmployeeNumber_shouldNotThrow_whenEmployeeNumberIsAvailable() {
        when(employeeProfileRepository.existsByEmployeeNumber("EMP001")).thenReturn(false);

        assertThatCode(() -> userPolicy.assertUniqueEmployeeNumber("EMP001")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertUniqueEmployeeNumber() - should throw DuplicateResourceException when employee number already exists")
    void assertUniqueEmployeeNumber_shouldThrow_whenEmployeeNumberAlreadyExists() {
        when(employeeProfileRepository.existsByEmployeeNumber("EMP001")).thenReturn(true);

        assertThatThrownBy(() -> userPolicy.assertUniqueEmployeeNumber("EMP001"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Employee number");
    }

    // --- assertPasswordStrength ---

    @Test
    @DisplayName("assertPasswordStrength() - should not throw for a strong password")
    void assertPasswordStrength_shouldNotThrow_forStrongPassword() {
        assertThatCode(() -> userPolicy.assertPasswordStrength("Secure1!")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertPasswordStrength() - should throw for null password")
    void assertPasswordStrength_shouldThrow_forNullPassword() {
        assertThatThrownBy(() -> userPolicy.assertPasswordStrength(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strength requirements");
    }

    @Test
    @DisplayName("assertPasswordStrength() - should throw for password shorter than 8 characters")
    void assertPasswordStrength_shouldThrow_forShortPassword() {
        assertThatThrownBy(() -> userPolicy.assertPasswordStrength("Ab1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strength requirements");
    }

    @Test
    @DisplayName("assertPasswordStrength() - should throw when password has no uppercase letter")
    void assertPasswordStrength_shouldThrow_whenNoUppercase() {
        assertThatThrownBy(() -> userPolicy.assertPasswordStrength("secure1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strength requirements");
    }

    @Test
    @DisplayName("assertPasswordStrength() - should throw when password has no lowercase letter")
    void assertPasswordStrength_shouldThrow_whenNoLowercase() {
        assertThatThrownBy(() -> userPolicy.assertPasswordStrength("SECURE1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strength requirements");
    }

    @Test
    @DisplayName("assertPasswordStrength() - should throw when password has no digit")
    void assertPasswordStrength_shouldThrow_whenNoDigit() {
        assertThatThrownBy(() -> userPolicy.assertPasswordStrength("Secure!!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strength requirements");
    }

    @Test
    @DisplayName("assertPasswordStrength() - should throw when password has no special character")
    void assertPasswordStrength_shouldThrow_whenNoSpecialChar() {
        assertThatThrownBy(() -> userPolicy.assertPasswordStrength("Secure12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strength requirements");
    }

    // --- assertActiveUser ---

    @Test
    @DisplayName("assertActiveUser() - should not throw for an active user")
    void assertActiveUser_shouldNotThrow_whenUserIsActive() {
        User user = makeUser(Role.CUSTOMER, true);

        assertThatCode(() -> userPolicy.assertActiveUser(user)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertActiveUser() - should throw DisabledException for an inactive user")
    void assertActiveUser_shouldThrow_whenUserIsInactive() {
        User user = makeUser(Role.CUSTOMER, false);

        assertThatThrownBy(() -> userPolicy.assertActiveUser(user))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("inactive");
    }

    // --- assertLoginAllowed ---

    @Test
    @DisplayName("assertLoginAllowed() - should not throw for an EMPLOYEE regardless of profile")
    void assertLoginAllowed_shouldNotThrow_forEmployee() {
        User user = makeUser(Role.EMPLOYEE, true);

        assertThatCode(() -> userPolicy.assertLoginAllowed(user)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertLoginAllowed() - should not throw for a CUSTOMER with APPROVED status")
    void assertLoginAllowed_shouldNotThrow_forApprovedCustomer() {
        User user = makeUser(Role.CUSTOMER, true);
        CustomerProfile profile = makeProfile(user, CustomerStatus.APPROVED);
        when(customerProfileRepository.findById(user.getId())).thenReturn(Optional.of(profile));

        assertThatCode(() -> userPolicy.assertLoginAllowed(user)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertLoginAllowed() - should not throw for a CUSTOMER with no profile yet")
    void assertLoginAllowed_shouldNotThrow_forCustomerWithNoProfile() {
        User user = makeUser(Role.CUSTOMER, true);
        when(customerProfileRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThatCode(() -> userPolicy.assertLoginAllowed(user)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertLoginAllowed() - should throw ForbiddenException for a REJECTED customer")
    void assertLoginAllowed_shouldThrow_forRejectedCustomer() {
        User user = makeUser(Role.CUSTOMER, true);
        CustomerProfile profile = makeProfile(user, CustomerStatus.REJECTED);
        when(customerProfileRepository.findById(user.getId())).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> userPolicy.assertLoginAllowed(user))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("no longer allowed");
    }

    @Test
    @DisplayName("assertLoginAllowed() - should throw ForbiddenException for a CLOSED customer account")
    void assertLoginAllowed_shouldThrow_forClosedCustomer() {
        User user = makeUser(Role.CUSTOMER, true);
        CustomerProfile profile = makeProfile(user, CustomerStatus.CLOSED);
        when(customerProfileRepository.findById(user.getId())).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> userPolicy.assertLoginAllowed(user))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("no longer allowed");
    }

    // --- helpers ---

    private User makeUser(Role role, boolean active) {
        User user = new User();
        user.setId(role == Role.EMPLOYEE ? 1L : 2L);
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    private CustomerProfile makeProfile(User user, CustomerStatus status) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setStatus(status);
        return profile;
    }
}
