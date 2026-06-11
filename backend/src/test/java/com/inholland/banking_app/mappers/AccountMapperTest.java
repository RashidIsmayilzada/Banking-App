package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccountMapperTest {

    private final AccountMapper accountMapper = new AccountMapper();

    @Test
    void toResponse_mapsEveryField() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("customer");
        owner.setEmail("john.doe@example.com");

        CustomerProfile profile = new CustomerProfile();
        profile.setFirstName("John");
        profile.setLastName("Doe");
        owner.setCustomerProfile(profile);

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

        assertEquals(1L, result.getOwnerId());
        assertEquals("customer", result.getOwnerUsername());
        assertEquals("John", result.getOwnerFirstName());
        assertEquals("Doe", result.getOwnerLastName());
        assertEquals("john.doe@example.com", result.getOwnerEmail());
        assertEquals("NL91ABNA0417164300", result.getIban());
        assertEquals(AccountType.CHECKING, result.getAccountType());
        // BigDecimal equals() is scale-sensitive (1500.50 != 1500.5); compare money by value.
        assertThat(result.getBalance().getAmount()).isEqualByComparingTo("1500.50");
        assertEquals("EUR", result.getBalance().getCurrency());
        assertThat(result.getAbsoluteTransferLimit().getAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.getDailyTransferLimit().getAmount()).isEqualByComparingTo("2000.00");
        assertEquals(AccountStatus.CLOSED, result.getStatus());
        assertEquals(createdAt, result.getCreatedAt());
        assertEquals(closedAt, result.getClosedAt());
    }

    @Test
    void toResponse_omitsNameAndClosedAt_whenProfileMissingAndAccountActive() {
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

        assertNull(result.getOwnerFirstName());
        assertNull(result.getOwnerLastName());
        assertNull(result.getClosedAt());
        assertEquals(AccountStatus.ACTIVE, result.getStatus());
    }
}
