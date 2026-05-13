package com.inholland.banking_app.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class AccountListResponse {

    private List<AccountResponse> accounts;
    private Totals totals;

    @Getter
    @AllArgsConstructor
    public static class Totals {
        private MoneyResponse combinedBalance;
    }

    public static AccountListResponse of(List<AccountResponse> accounts) {
        BigDecimal combined = accounts.stream()
                .map(a -> a.getBalance().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AccountListResponse(accounts, new Totals(MoneyResponse.eur(combined)));
    }
}
