package com.inholland.banking_app.models.factory;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AccountFactory {

    private static final BigDecimal OPENING_BALANCE = BigDecimal.ZERO;
    private static final BigDecimal NO_OVERDRAFT_LIMIT = BigDecimal.ZERO;
    private static final BigDecimal CHECKING_DAILY_LIMIT = new BigDecimal("1000.00");
    private static final BigDecimal SAVINGS_DAILY_LIMIT = new BigDecimal("5000.00");

    private AccountFactory() {
    }

    public static Account createCheckingAccount(User customer, String iban) {
        return createAccount(customer, iban, AccountType.CHECKING, CHECKING_DAILY_LIMIT, LocalDateTime.now());
    }

    public static Account createSavingsAccount(User customer, String iban) {
        return createAccount(customer, iban, AccountType.SAVINGS, SAVINGS_DAILY_LIMIT, LocalDateTime.now());
    }

    private static Account createAccount(
            User customer,
            String iban,
            AccountType accountType,
            BigDecimal dailyTransferLimit,
            LocalDateTime now) {
        Account account = new Account();
        account.setCustomer(customer);
        account.setIban(iban);
        account.setAccountType(accountType);
        account.setBalance(OPENING_BALANCE);
        account.setAbsoluteTransferLimit(NO_OVERDRAFT_LIMIT);
        account.setDailyTransferLimit(dailyTransferLimit);
        account.setActive(true);
        account.setCreatedAt(now);
        return account;
    }
}
