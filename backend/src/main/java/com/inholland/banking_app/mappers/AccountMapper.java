package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.models.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toAccountResponse(Account account) {

        if (account == null) {
            return null;
        }

        AccountResponse response = new AccountResponse();
        response.setId(account.getId());

        var customer = account.getCustomer();
        if (customer != null) {
            response.setCustomerId(customer.getId());
        } else {
            response.setCustomerId(null);
        }
        response.setIban(account.getIban());
        response.setAccountType(account.getAccountType());
        response.setBalance(account.getBalance());
        response.setAbsoluteTransferLimit(account.getAbsoluteTransferLimit());
        response.setDailyTransferLimit(account.getDailyTransferLimit());
        response.setActive(account.isActive());
        response.setCreatedAt(account.getCreatedAt());

        if (account.getClosedAt() != null) {
            response.setClosedAt(account.getClosedAt());
        }
        return response;
    }
}
