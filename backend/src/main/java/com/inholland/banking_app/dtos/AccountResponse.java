package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private Long id;
    private Long customerId;
    private String iban;
    private AccountType accountType;
    private BigDecimal balance;
    private BigDecimal absoluteTransferLimit;
    private BigDecimal dailyTransferLimit;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

}
