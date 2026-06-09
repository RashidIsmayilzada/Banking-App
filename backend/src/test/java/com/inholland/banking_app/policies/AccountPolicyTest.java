package com.inholland.banking_app.policies;

import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountPolicyTest {

    private AccountPolicy policy;
    private Account account;

    @BeforeEach
    void setUp() {
        policy = new AccountPolicy();

        account = new Account();
        account.setIban("NL91ABNA0417164300");
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
}
