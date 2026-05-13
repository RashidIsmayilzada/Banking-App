package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    public AccountListResponse listAccounts(Long userId) {
        User currentUser = getCurrentUser();
        List<Account> accounts;

        if (currentUser.getRole() == Role.EMPLOYEE) {
            accounts = (userId != null)
                    ? accountRepository.findByCustomerId(userId)
                    : accountRepository.findAll();
        } else {
            accounts = accountRepository.findByCustomerId(currentUser.getId());
        }

        List<AccountResponse> responses = accounts.stream()
                .map(accountMapper::toResponse)
                .toList();

        return AccountListResponse.of(responses);
    }

    public AccountResponse getAccount(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.CUSTOMER
                && !account.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have access to this account");
        }

        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long accountId, AccountUpdateRequest request) {
        Account account = findAccountOrThrow(accountId);

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

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }
}