package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountListResponseTest {

    @Test
    void of_sumsBalancesOfEveryAccountOnThePage() {
        Page<AccountResponse> page = new PageImpl<>(List.of(
                accountWithBalance("1000.00"),
                accountWithBalance("2500.50"),
                accountWithBalance("500.25")
        ), PageRequest.of(0, 10), 3);

        AccountListResponse result = AccountListResponse.of(page);

        // BigDecimal equals() is scale-sensitive; compare money by value.
        assertThat(result.getTotals().getCombinedBalance().getAmount()).isEqualByComparingTo("4000.75");
        assertEquals("EUR", result.getTotals().getCombinedBalance().getCurrency());
        assertEquals(3, result.getAccounts().size());
    }

    @Test
    void of_returnsZeroCombinedBalance_whenPageIsEmpty() {
        Page<AccountResponse> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        AccountListResponse result = AccountListResponse.of(page);

        assertThat(result.getTotals().getCombinedBalance().getAmount()).isEqualByComparingTo("0");
        assertTrue(result.getAccounts().isEmpty());
    }

    @Test
    void of_copiesPageMetadata() {
        Page<AccountResponse> page = new PageImpl<>(List.of(accountWithBalance("1000.00")), PageRequest.of(2, 5), 25);

        AccountListResponse result = AccountListResponse.of(page);

        assertEquals(2, result.getPage().getCurrentPage());
        assertEquals(5, result.getPage().getPageSize());
        assertEquals(25, result.getPage().getTotalElements());
        assertEquals(5, result.getPage().getTotalPages());
    }

    private AccountResponse accountWithBalance(String balance) {
        return AccountResponse.builder()
                .ownerId(1L)
                .ownerUsername("customer")
                .iban("NL91ABNA0417164300")
                .accountType(AccountType.CHECKING)
                .balance(MoneyResponse.eur(new BigDecimal(balance)))
                .absoluteTransferLimit(MoneyResponse.eur(new BigDecimal("5000.00")))
                .dailyTransferLimit(MoneyResponse.eur(new BigDecimal("2000.00")))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
