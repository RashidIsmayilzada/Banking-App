package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.MoneyResponse;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        User owner = account.getCustomer();
        // Personal details live on the profile; guard it so the mapper still works
        // for owners that have no customer profile.
        CustomerProfile profile = owner.getCustomerProfile();

        return AccountResponse.builder()
                .ownerId(owner.getId())
                .ownerUsername(owner.getUsername())
                .ownerFirstName(profile != null ? profile.getFirstName() : null)
                .ownerLastName(profile != null ? profile.getLastName() : null)
                .ownerEmail(owner.getEmail())
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