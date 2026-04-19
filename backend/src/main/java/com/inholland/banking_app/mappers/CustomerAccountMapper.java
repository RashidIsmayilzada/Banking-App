package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.customer.AccountTotalsResponse;
import com.inholland.banking_app.dtos.customer.CustomerAccountResponse;
import com.inholland.banking_app.models.Account;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
@Component
public class CustomerAccountMapper {

    public CustomerAccountResponse toResponse(Account account) {
        CustomerAccountResponse response = new CustomerAccountResponse();
        response.setAccountId(account.getId());
        response.setIban(account.getIban());
        response.setAccountType(account.getAccountType());
        response.setBalance(account.getBalance());
        response.setAbsoluteTransferLimit(account.getAbsoluteTransferLimit());
        response.setDailyTransferLimit(account.getDailyTransferLimit());
        response.setActive(account.isActive());
        response.setCreatedAt(account.getCreatedAt());
        response.setClosedAt(account.getClosedAt());
        return response;
    }

    public AccountTotalsResponse toTotalsResponse(BigDecimal combinedBalance) {
        AccountTotalsResponse response = new AccountTotalsResponse();
        response.setCombinedBalance(combinedBalance);
        return response;
    }
}
