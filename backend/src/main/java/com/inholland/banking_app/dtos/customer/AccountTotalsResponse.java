package com.inholland.banking_app.dtos.customer;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountTotalsResponse {
    private BigDecimal combinedBalance;
}
