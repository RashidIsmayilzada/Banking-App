package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.repositories.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
}
