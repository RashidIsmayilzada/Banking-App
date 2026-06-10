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

    // combinedBalance is summed in the database over every matching account
    public static AccountListResponse of(Page<AccountResponse> page, BigDecimal combinedBalance) {
        return new AccountListResponse(
                page.getContent(),
                new Totals(MoneyResponse.eur(combinedBalance)),
                new PageInfo(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                )
        );
    }
}
