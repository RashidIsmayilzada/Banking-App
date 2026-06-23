package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    // --Efe(Admin)
    private final AuditService auditService;
    // --Efe(Admin)
    private final UserRepository userRepository;

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

    // --Efe(Admin)
    @Transactional
    public AccountResponse freezeAccount(String iban) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + iban));

        if (account.isClosed()) {
            throw new AccountStateException("Cannot freeze a closed account");
        }
        if (account.isFrozen()) {
            throw new AccountStateException("Account is already frozen");
        }

        account.markFrozen();
        accountRepository.save(account);

        auditService.record(getCurrentAdmin(), AuditAction.ACCOUNT_FROZEN, "ACCOUNT", null, "Froze account: " + iban);

        return accountMapper.toResponse(account);
    }

    // --Efe(Admin)
    @Transactional
    public AccountResponse unfreezeAccount(String iban) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + iban));

        if (!account.isFrozen()) {
            throw new AccountStateException("Account is not frozen");
        }

        account.unfreeze();
        accountRepository.save(account);

        auditService.record(getCurrentAdmin(), AuditAction.ACCOUNT_UNFROZEN, "ACCOUNT", null, "Unfroze account: " + iban);

        return accountMapper.toResponse(account);
    }

    // --Efe(Admin)
    @Transactional
    public AccountResponse closeAccount(String iban) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + iban));

        if (account.isClosed()) {
            throw new AccountStateException("Account is already closed");
        }

        account.markClosed();
        accountRepository.save(account);

        auditService.record(getCurrentAdmin(), AuditAction.ACCOUNT_CLOSED, "ACCOUNT", null, "Closed account: " + iban);

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

    // --Efe(Admin)
    // Get current admin user if not crash it
    private User getCurrentAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current authenticated admin user not found"));
    }
}
