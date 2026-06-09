package com.inholland.banking_app.policies;

import com.inholland.banking_app.dtos.TransactionRequest;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.DailyTransferUsage;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class TransactionPolicy {

    private final DailyTransferUsageRepository dailyTransferUsageRepository;

    public void validateTransferFields(TransactionRequest request) {
        if (request.getFromIban() == null) {
            throw new IllegalArgumentException("fromIban is required for TRANSFER");
        }
        if (request.getToIban() == null) {
            throw new IllegalArgumentException("toIban is required for TRANSFER");
        }
    }

    public void requireIban(TransactionRequest request, String type) {
        if (request.getIban() == null) {
            throw new IllegalArgumentException("iban is required for " + type);
        }
    }

    public void validateActiveAccount(Account account, String message) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(message);
        }
    }

    public void validateCheckingAccount(Account account) {
        if (!account.getAccountType().canInitiateTransfer()) {
            throw new IllegalArgumentException("Transfers can only be made from a checking account");
        }
    }

    public void validateAccountOwnership(Account account, User currentUser, String message) {
        if (currentUser.getRole() == Role.CUSTOMER
                && !account.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException(message);
        }
    }

    public void checkBalance(Account account, BigDecimal amount) {
        if (account.getBalance().subtract(amount).compareTo(account.getAbsoluteTransferLimit()) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }

    public void checkDailyLimit(Account account, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        BigDecimal usedToday = dailyTransferUsageRepository
                .findByAccountIbanAndUsageDate(account.getIban(), today)
                .map(DailyTransferUsage::getTotalOutgoingAmount)
                .orElse(BigDecimal.ZERO);
        if (usedToday.add(amount).compareTo(account.getDailyTransferLimit()) > 0) {
            throw new IllegalArgumentException("Daily transfer limit exceeded");
        }
    }
}
