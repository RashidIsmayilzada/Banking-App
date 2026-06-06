package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.MoneyResponse;
import com.inholland.banking_app.models.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getId())
                .ownerId(account.getCustomer().getId())
                .ownerUsername(account.getCustomer().getUsername())
                .iban(account.getIban())
                .accountType(account.getAccountType())
                .balance(MoneyResponse.eur(account.getBalance()))
                .absoluteTransferLimit(MoneyResponse.eur(account.getAbsoluteTransferLimit()))
                .dailyTransferLimit(MoneyResponse.eur(account.getDailyTransferLimit()))
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .closedAt(account.getClosedAt())
                .build();
    }
}