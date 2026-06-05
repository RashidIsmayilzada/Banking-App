package com.inholland.banking_app.models.factory;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TransactionFactory {

    private TransactionFactory() {}

    public static Transaction createTransfer(Account from, Account to, BigDecimal amount,
                                             User initiatedBy, Channel channel, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setFromAccount(from);
        transaction.setToAccount(to);
        transaction.setAmount(amount);
        transaction.setCurrency("EUR");
        transaction.setChannel(channel);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(description);
        return transaction;
    }

    public static Transaction createDeposit(Account account, BigDecimal amount,
                                            User initiatedBy, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setToAccount(account);
        transaction.setAmount(amount);
        transaction.setCurrency("EUR");
        transaction.setChannel(Channel.ATM);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(description);
        return transaction;
    }

    public static Transaction createWithdrawal(Account account, BigDecimal amount,
                                               User initiatedBy, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setFromAccount(account);
        transaction.setAmount(amount);
        transaction.setCurrency("EUR");
        transaction.setChannel(Channel.ATM);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(description);
        return transaction;
    }
}
