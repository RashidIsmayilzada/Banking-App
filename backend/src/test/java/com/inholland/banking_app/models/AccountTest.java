package com.inholland.banking_app.models;

import com.inholland.banking_app.models.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Account entity only holds state and performs raw mutations.
 * Business rules (when a limit may change, when an account may close,
 * ownership/access) are covered by {@code AccountPolicyTest}.
 */
class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setIban("NL91ABNA0417164300");
        account.setStatus(AccountStatus.ACTIVE);
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("applyLimits() - should update both limits when both are provided")
    void applyLimits_shouldUpdateBothLimits_whenBothProvided() {
        account.applyLimits(new BigDecimal("8000.00"), new BigDecimal("3000.00"));

        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("applyLimits() - should update only absolute limit when daily is null")
    void applyLimits_shouldUpdateOnlyAbsolute_whenDailyIsNull() {
        account.applyLimits(new BigDecimal("8000.00"), null);

        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("applyLimits() - should update only daily limit when absolute is null")
    void applyLimits_shouldUpdateOnlyDaily_whenAbsoluteIsNull() {
        account.applyLimits(null, new BigDecimal("3000.00"));

        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("5000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("markClosed() - should set status to CLOSED and set closedAt")
    void markClosed_shouldSetStatusAndClosedAt() {
        account.markClosed();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(account.getClosedAt()).isNotNull();
    }
}
