package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.policies.AccountPolicy;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.UserRepository;
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
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    private final AccountPolicy accountPolicy;

    public AccountListResponse listAccounts(Long userId, String username, Pageable pageable) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Page<Account> accounts;

        if (currentUser.getRole() == Role.CUSTOMER) {
            accounts = accountRepository.findByCustomerId(currentUser.getId(), pageable);
        } else {
            accounts = (userId != null)
                    ? accountRepository.findByCustomerId(userId, pageable)
                    : accountRepository.findAll(pageable);
        }

        Page<AccountResponse> responses = accounts.map(accountMapper::toResponse);

        return AccountListResponse.of(responses);
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