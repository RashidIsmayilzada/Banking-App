package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
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

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountPolicy accountPolicy;

    /**
     * Lists accounts, optionally narrowed to a single customer. The caller
     * decides the scope; this method applies no role-based rules.
     */
    public AccountListResponse listAccounts(Long customerId, Pageable pageable) {
        Page<Account> accounts = (customerId != null)
                ? accountRepository.findByCustomerId(customerId, pageable)
                : accountRepository.findAll(pageable);

        return toListResponse(accounts);
    }

    /**
     * Lists the accounts owned by the given username.
     */
    public AccountListResponse listAccountsOwnedBy(String username, Pageable pageable) {
        return toListResponse(accountRepository.findByCustomerUsername(username, pageable));
    }

    private AccountListResponse toListResponse(Page<Account> accounts) {
        return AccountListResponse.of(accounts.map(accountMapper::toResponse));
    }

    public AccountResponse getAccount(String iban) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(String iban, AccountUpdateRequest request) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

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
}