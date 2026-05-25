package com.inholland.banking_app.models;

import com.inholland.banking_app.models.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private Account account;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);

        account = new Account();
        account.setId(10L);
        account.setCustomer(owner);
        account.setStatus(AccountStatus.ACTIVE);
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("isOwnedBy() - should return true when user is the owner")
    void isOwnedBy_shouldReturnTrue_whenUserIsOwner() {
        assertThat(account.isOwnedBy(owner)).isTrue();
    }

    @Test
    @DisplayName("isOwnedBy() - should return false when user is not the owner")
    void isOwnedBy_shouldReturnFalse_whenUserIsNotOwner() {
        User other = new User();
        other.setId(99L);

        assertThat(account.isOwnedBy(other)).isFalse();
    }

    @Test
    @DisplayName("isClosed() - should return false when account is active")
    void isClosed_shouldReturnFalse_whenAccountIsActive() {
        assertThat(account.isClosed()).isFalse();
    }

    @Test
    @DisplayName("isClosed() - should return true when account is closed")
    void isClosed_shouldReturnTrue_whenAccountIsClosed() {
        account.setStatus(AccountStatus.CLOSED);

        assertThat(account.isClosed()).isTrue();
    }

    @Test
    @DisplayName("updateLimits() - should update both limits when both are provided")
    void updateLimits_shouldUpdateBothLimits_whenBothProvided() {
        account.updateLimits(new BigDecimal("8000.00"), new BigDecimal("3000.00"));

        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("updateLimits() - should update only absolute limit when daily is null")
    void updateLimits_shouldUpdateOnlyAbsolute_whenDailyIsNull() {
        account.updateLimits(new BigDecimal("8000.00"), null);

        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("updateLimits() - should update only daily limit when absolute is null")
    void updateLimits_shouldUpdateOnlyDaily_whenAbsoluteIsNull() {
        account.updateLimits(null, new BigDecimal("3000.00"));

        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("5000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("updateLimits() - should throw IllegalStateException when account is closed")
    void updateLimits_shouldThrow_whenAccountIsClosed() {
        account.setStatus(AccountStatus.CLOSED);

        assertThatThrownBy(() -> account.updateLimits(new BigDecimal("8000.00"), new BigDecimal("3000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("close() - should set status to CLOSED and set closedAt")
    void close_shouldSetStatusAndClosedAt() {
        account.close();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(account.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("close() - should throw IllegalStateException when account is already closed")
    void close_shouldThrow_whenAlreadyClosed() {
        account.setStatus(AccountStatus.CLOSED);

        assertThatThrownBy(() -> account.close())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }
}
