package com.inholland.banking_app.policies;

import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountPolicyTest {

    private AccountPolicy policy;
    private Account account;
    private User owner;

    @BeforeEach
    void setUp() {
        policy = new AccountPolicy();

        owner = new User();
        owner.setId(1L);
        owner.setRole(Role.CUSTOMER);

        account = new Account();
        account.setIban("NL91ABNA0417164300");
        account.setCustomer(owner);
        account.setStatus(AccountStatus.ACTIVE);
    }

    // --- assertCanUpdateLimits ---

    @Test
    @DisplayName("assertCanUpdateLimits() - should not throw when account is active")
    void assertCanUpdateLimits_shouldNotThrow_whenAccountIsActive() {
        assertThatCode(() -> policy.assertCanUpdateLimits(account)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanUpdateLimits() - should throw AccountStateException when account is closed")
    void assertCanUpdateLimits_shouldThrow_whenAccountIsClosed() {
        account.setStatus(AccountStatus.CLOSED);

        assertThatThrownBy(() -> policy.assertCanUpdateLimits(account))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("closed");
    }

    // --- assertCanClose ---

    @Test
    @DisplayName("assertCanClose() - should not throw when account is active")
    void assertCanClose_shouldNotThrow_whenAccountIsActive() {
        assertThatCode(() -> policy.assertCanClose(account)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanClose() - should throw AccountStateException when account is already closed")
    void assertCanClose_shouldThrow_whenAlreadyClosed() {
        account.setStatus(AccountStatus.CLOSED);

        assertThatThrownBy(() -> policy.assertCanClose(account))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("already closed");
    }

    // --- isOwnedBy ---

    @Test
    @DisplayName("isOwnedBy() - should return true when user is the owner")
    void isOwnedBy_shouldReturnTrue_whenUserIsOwner() {
        assertThat(policy.isOwnedBy(account, owner)).isTrue();
    }

    @Test
    @DisplayName("isOwnedBy() - should return false when user is not the owner")
    void isOwnedBy_shouldReturnFalse_whenUserIsNotOwner() {
        User other = new User();
        other.setId(99L);

        assertThat(policy.isOwnedBy(account, other)).isFalse();
    }

    // --- canAccess ---

    @Test
    @DisplayName("canAccess() - should return true for the owning customer")
    void canAccess_shouldReturnTrue_forOwningCustomer() {
        assertThat(policy.canAccess(account, owner)).isTrue();
    }

    @Test
    @DisplayName("canAccess() - should return false for a non-owning customer")
    void canAccess_shouldReturnFalse_forNonOwningCustomer() {
        User other = new User();
        other.setId(99L);
        other.setRole(Role.CUSTOMER);

        assertThat(policy.canAccess(account, other)).isFalse();
    }

    @Test
    @DisplayName("canAccess() - should return true for any employee")
    void canAccess_shouldReturnTrue_forEmployee() {
        User employee = new User();
        employee.setId(99L);
        employee.setRole(Role.EMPLOYEE);

        assertThat(policy.canAccess(account, employee)).isTrue();
    }
}
