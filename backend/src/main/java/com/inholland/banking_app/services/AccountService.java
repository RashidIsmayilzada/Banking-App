package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.factory.AccountFactory;
import com.inholland.banking_app.repositories.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    // --- Reads ---

    // Lists all accounts, or just one customer's when customerId is given.
    public AccountListResponse listAccounts(Long customerId, Pageable pageable) {
        Page<Account> accounts;
        if (customerId != null) {
            accounts = accountRepository.findByCustomerId(customerId, pageable);
        } else {
            accounts = accountRepository.findAll(pageable);
        }
        return AccountListResponse.of(accounts.map(accountMapper::toResponse));
    }

    // Lists the accounts owned by the given username.
    public AccountListResponse listAccountsOwnedBy(String username, Pageable pageable) {
        Page<Account> accounts = accountRepository.findByCustomerUsername(username, pageable);
        return AccountListResponse.of(accounts.map(accountMapper::toResponse));
    }

    // Returns a single account by IBAN.
    public AccountResponse getAccount(String iban) {
        Account account = findAccountOrThrow(iban);
        return accountMapper.toResponse(account);
    }

    // --- Updates ---

    // Applies limit changes and/or closes the account, enforcing account rules.
    @Transactional
    public AccountResponse updateAccount(String iban, AccountUpdateRequest request) {
        Account account = findAccountOrThrow(iban);

        // A closed account is frozen: its limits and status can no longer change.
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountStateException("Cannot update a closed account");
        }

        if (request.getAbsoluteTransferLimit() != null) {
            account.setAbsoluteTransferLimit(request.getAbsoluteTransferLimit());
        }
        if (request.getDailyTransferLimit() != null) {
            account.setDailyTransferLimit(request.getDailyTransferLimit());
        }
        if (request.getStatus() == AccountStatus.CLOSED) {
            account.setStatus(AccountStatus.CLOSED);
            account.setClosedAt(LocalDateTime.now());
        }
        accountRepository.save(account);
        return accountMapper.toResponse(account);
    }

    // --- Helpers ---

    private Account findAccountOrThrow(String iban) {
        return accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    // ==================== Amazing code ====================

    // Creates the default CHECKING + SAVINGS accounts and 
    // apply the account limit for a newly approved customer
    public void createDefaultAccounts(User user, ApproveCustomerRequest approveCustomer) {
        createAccount(user, AccountType.CHECKING, approveCustomer.getCheckingAbsoluteLimit(), approveCustomer.getCheckingDailyLimit());
        createAccount(user, AccountType.SAVINGS, null, approveCustomer.getSavingsDailyLimit());
    }

    public void closeAllAccounts(User user) {
        // Get all the user account and set status to close
        // and set the datetime
        for (Account account : user.getAccounts()) {
            account.setStatus(AccountStatus.CLOSED);
            account.setClosedAt(LocalDateTime.now());
        }
    }

    public void reopenAllAccounts(User user) {
        for (Account account : user.getAccounts()) {
            account.setStatus(AccountStatus.ACTIVE);
            account.setClosedAt(null);
        }
    }

    // Create Bank Account with Iban
    private void createAccount(User user, AccountType accountType,
            BigDecimal customAbsoluteLimit, BigDecimal customDailyLimit) {
        // generate Iban according to Netherlands standard
        String iban = generateIban(user.getId(), accountType);
        // Use a linear operation to create two account account type
        Account account = accountType == AccountType.CHECKING
                ? AccountFactory.createCheckingAccount(user, iban)
                : AccountFactory.createSavingsAccount(user, iban);

        // Apply account Limits
        account.applyLimits(customAbsoluteLimit, customDailyLimit);

        // add account to user not DB yet
        user.getAccounts().add(account);
    }

    private String generateIban(Long userId, AccountType accountType) {
        long accountNumber = userId * 10 + (accountType == AccountType.CHECKING ? 1 : 2);
        String iban = String.format("NL%02dINHO%010d", accountType == AccountType.CHECKING ? 10 : 20, accountNumber);

        while (accountRepository.existsByIban(iban)) {
            accountNumber++;
            iban = String.format("NL%02dINHO%010d", accountType == AccountType.CHECKING ? 10 : 20, accountNumber);
        }

        return iban;
    }
}