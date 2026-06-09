package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    private final AccountMapper accountMapper = new AccountMapper();

    @Test
    @DisplayName("toResponse() - should map all fields correctly")
    void toResponse_shouldMapAllFieldsCorrectly() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("customer");

        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime closedAt = LocalDateTime.of(2025, 5, 20, 14, 30);

        Account account = new Account();
        account.setCustomer(owner);
        account.setIban("NL91ABNA0417164300");
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("1500.50"));
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
        account.setStatus(AccountStatus.CLOSED);
        account.setCreatedAt(createdAt);
        account.setClosedAt(closedAt);

        AccountResponse result = accountMapper.toResponse(account);

        assertThat(result.getOwnerId()).isEqualTo(1L);
        assertThat(result.getOwnerUsername()).isEqualTo("customer");
        assertThat(result.getIban()).isEqualTo("NL91ABNA0417164300");
        assertThat(result.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(result.getBalance().getAmount()).isEqualByComparingTo("1500.50");
        assertThat(result.getBalance().getCurrency()).isEqualTo("EUR");
        assertThat(result.getAbsoluteTransferLimit().getAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.getDailyTransferLimit().getAmount()).isEqualByComparingTo("2000.00");
        assertThat(result.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getClosedAt()).isEqualTo(closedAt);
    }

    @Test
    @DisplayName("toResponse() - should map closedAt as null when account is active")
    void toResponse_shouldMapClosedAtAsNull_whenAccountIsActive() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("customer");

        Account account = new Account();
        account.setCustomer(owner);
        account.setIban("NL91ABNA0417164300");
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(new BigDecimal("500.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());

        AccountResponse result = accountMapper.toResponse(account);

        assertThat(result.getClosedAt()).isNull();
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}
