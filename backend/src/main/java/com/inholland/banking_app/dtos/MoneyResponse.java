package com.inholland.banking_app.dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MoneyResponse {
    private BigDecimal amount;
    private String currency;
}
