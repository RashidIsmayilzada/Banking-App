package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountSearchResult;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
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
import java.util.List;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    // --- Reads ---

    // Lists all accounts, or just one customer's when customerId is given.
    public AccountListResponse listAccounts(Long customerId, Pageable pageable) {
        Page<Account> accounts = (customerId != null)
                ? accountRepository.findByCustomerId(customerId, pageable)
                : accountRepository.findAll(pageable);
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

    @Transactional(readOnly = true)
    public List<AccountSearchResult> searchByCustomerName(String name) {
        return accountRepository.searchCheckingByCustomerName(name).stream()
                .map(a -> {
                    var profile = a.getCustomer().getCustomerProfile();
                    String fullName = profile != null
                            ? profile.getFirstName() + " " + profile.getLastName()
                            : a.getCustomer().getUsername();
                    return new AccountSearchResult(a.getIban(), fullName);
                })
                .toList();
    }

    // --- Updates ---

    // Applies limit changes and/or closes the account, enforcing account rules.
    @Transactional
    public AccountResponse updateAccount(String iban, AccountUpdateRequest request) {
        Account account = findAccountOrThrow(iban);

        assertCanUpdateLimits(account);
        if (request.getAbsoluteTransferLimit() != null) {
            account.setAbsoluteTransferLimit(request.getAbsoluteTransferLimit());
        }
        if (request.getDailyTransferLimit() != null) {
            account.setDailyTransferLimit(request.getDailyTransferLimit());
        }
        if (AccountStatus.CLOSED.equals(request.getStatus())) {
            assertCanClose(account);
            account.setStatus(AccountStatus.CLOSED);
            account.setClosedAt(LocalDateTime.now());
        } else if (AccountStatus.ACTIVE.equals(request.getStatus())) {
            account.setStatus(AccountStatus.ACTIVE);
            account.setClosedAt(null);
        }
        accountRepository.save(account);
        return accountMapper.toResponse(account);
    }

    // --- Helpers ---

    private Account findAccountOrThrow(String iban) {
        return accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    // A closed account is frozen: its transfer limits can no longer be changed.
    private void assertCanUpdateLimits(Account account) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountStateException("Cannot update a closed account");
        }
    }

    // An account can only be closed once.
    private void assertCanClose(Account account) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountStateException("Account is already closed");
        }
    }

    // ==================== Amazing code ====================

    // Creates the default CHECKING + SAVINGS accounts for a newly approved customer.
    public void createDefaultAccounts(User user, BigDecimal checkingAbsoluteLimit,
                                      BigDecimal checkingDailyLimit, BigDecimal savingsDailyLimit) {
        createAccount(user, AccountType.CHECKING, checkingAbsoluteLimit, checkingDailyLimit);
        createAccount(user, AccountType.SAVINGS, null, savingsDailyLimit);
    }

    // True when the user has no accounts yet (used to keep approval idempotent).
    public boolean hasNoAccounts(User user) {
        return accountRepository.findByCustomerId(user.getId(), Pageable.unpaged()).isEmpty();
    }

    private void applyLimits(Account account, BigDecimal absolute, BigDecimal daily) {
        if (absolute != null) account.setAbsoluteTransferLimit(absolute);
        if (daily != null) account.setDailyTransferLimit(daily);
    }

    public void closeAllAccounts(User user) {
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

    private void createAccount(User user, AccountType accountType,
                               BigDecimal customAbsoluteLimit, BigDecimal customDailyLimit) {
        String iban = generateIban(user.getId(), accountType);
        Account account = accountType == AccountType.CHECKING
                ? AccountFactory.createCheckingAccount(user, iban)
                : AccountFactory.createSavingsAccount(user, iban);

        applyLimits(account, customAbsoluteLimit, customDailyLimit);

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
