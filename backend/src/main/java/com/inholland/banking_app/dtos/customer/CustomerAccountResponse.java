package com.inholland.banking_app.dtos.customer;

import com.inholland.banking_app.models.enums.AccountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerAccountResponse {
    private Long accountId;
    private String iban;
    private AccountType accountType;
    private BigDecimal balance;
    private BigDecimal absoluteTransferLimit;
    private BigDecimal dailyTransferLimit;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
