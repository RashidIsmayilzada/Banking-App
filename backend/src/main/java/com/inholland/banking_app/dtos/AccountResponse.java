package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AccountResponse {

    private Long accountId;
    private Long ownerId;
    private String ownerUsername;
    private String iban;
    private AccountType accountType;
    private MoneyResponse balance;
    private MoneyResponse absoluteTransferLimit;
    private MoneyResponse dailyTransferLimit;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
