package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountListResponseTest {

    @Test
    @DisplayName("of() - should expose the combined balance it is given")
    void of_shouldExposeGivenCombinedBalance() {
        List<AccountResponse> accounts = List.of(
                buildAccountResponse(new BigDecimal("1000.00")),
                buildAccountResponse(new BigDecimal("2500.50")),
                buildAccountResponse(new BigDecimal("500.25"))
        );
        Page<AccountResponse> page = new PageImpl<>(accounts, PageRequest.of(0, 10), 3);

        // The total is summed in the database (across all pages) and passed in,
        // so it can legitimately differ from the sum of this page's accounts.
        AccountListResponse result = AccountListResponse.of(page, new BigDecimal("4000.75"));

        assertThat(result.getTotals().getCombinedBalance().getAmount())
                .isEqualByComparingTo("4000.75");
        assertThat(result.getTotals().getCombinedBalance().getCurrency())
                .isEqualTo("EUR");
        assertThat(result.getAccounts()).hasSize(3);
    }

    @Test
    @DisplayName("of() - should return zero combined balance when page is empty")
    void of_shouldReturnZeroBalance_whenPageIsEmpty() {
        Page<AccountResponse> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        AccountListResponse result = AccountListResponse.of(page, BigDecimal.ZERO);

        assertThat(result.getTotals().getCombinedBalance().getAmount())
                .isEqualByComparingTo("0");
        assertThat(result.getAccounts()).isEmpty();
    }

    @Test
    @DisplayName("of() - should set page info correctly")
    void of_shouldSetPageInfoCorrectly() {
        List<AccountResponse> accounts = List.of(buildAccountResponse(new BigDecimal("1000.00")));
        Page<AccountResponse> page = new PageImpl<>(accounts, PageRequest.of(2, 5), 25);

        AccountListResponse result = AccountListResponse.of(page, new BigDecimal("1000.00"));

        assertThat(result.getPage().getCurrentPage()).isEqualTo(2);
        assertThat(result.getPage().getPageSize()).isEqualTo(5);
        assertThat(result.getPage().getTotalElements()).isEqualTo(25);
        assertThat(result.getPage().getTotalPages()).isEqualTo(5);
    }

    private AccountResponse buildAccountResponse(BigDecimal balance) {
        return AccountResponse.builder()
                .ownerId(1L)
                .ownerUsername("customer")
                .iban("NL91ABNA0417164300")
                .accountType(AccountType.CHECKING)
                .balance(MoneyResponse.eur(balance))
                .absoluteTransferLimit(MoneyResponse.eur(new BigDecimal("5000.00")))
                .dailyTransferLimit(MoneyResponse.eur(new BigDecimal("2000.00")))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
