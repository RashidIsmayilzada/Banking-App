package com.inholland.banking_app.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MoneyResponse {

    private BigDecimal amount;
    private String currency;

    public static MoneyResponse eur(BigDecimal amount) {
        return new MoneyResponse(amount, "EUR");
    }
}
