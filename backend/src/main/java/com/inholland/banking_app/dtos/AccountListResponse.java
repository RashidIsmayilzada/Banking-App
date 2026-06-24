package com.inholland.banking_app.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class AccountListResponse {

    private List<AccountResponse> accounts;
    private Totals totals;
    private PageInfo page;

    @Getter
    @AllArgsConstructor
    public static class Totals {
        private MoneyResponse combinedBalance;
    }

    @Getter
    @AllArgsConstructor
    public static class PageInfo {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
    }

    public static AccountListResponse of(Page<AccountResponse> page) {
        // 1. Extract the list of children (A.objects)
        List<AccountResponse> accounts = page.getContent();
        // 2. Loop to get combined balance
        BigDecimal combined = BigDecimal.ZERO;
        for (AccountResponse accountResponse : accounts) {
            combined = combined.add(accountResponse.getBalance().getAmount());
        }

        return new AccountListResponse(
                accounts,
                new Totals(MoneyResponse.eur(combined)),
                new PageInfo(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                )
        );
    }
}
