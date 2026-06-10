package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountSearchResult;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.policies.AccountPolicy;
import com.inholland.banking_app.repositories.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountPolicy accountPolicy;

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

        accountPolicy.assertCanUpdateLimits(account);
        if (request.getAbsoluteTransferLimit() != null) {
            account.setAbsoluteTransferLimit(request.getAbsoluteTransferLimit());
        }
        if (request.getDailyTransferLimit() != null) {
            account.setDailyTransferLimit(request.getDailyTransferLimit());
        }
        if (AccountStatus.CLOSED.equals(request.getStatus())) {
            accountPolicy.assertCanClose(account);
            account.setStatus(AccountStatus.CLOSED);
            account.setClosedAt(LocalDateTime.now());
        }
        accountRepository.save(account);
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

    // --- Helpers ---

    private Account findAccountOrThrow(String iban) {
        return accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }
}