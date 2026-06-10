package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.dtos.MoneyResponse;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.policies.AccountPolicy;
import com.inholland.banking_app.repositories.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private AccountMapper accountMapper;
    @Mock private AccountPolicy accountPolicy;

    @InjectMocks private AccountService accountService;

    private User customer;
    private Account account;
    private AccountResponse accountResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setUsername("customer");

        account = new Account();
        account.setCustomer(customer);
        account.setIban("NL91ABNA0417164300");
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());

        accountResponse = AccountResponse.builder()
                .ownerId(1L)
                .ownerUsername("customer")
                .iban("NL91ABNA0417164300")
                .accountType(AccountType.CHECKING)
                .balance(MoneyResponse.eur(new BigDecimal("1000.00")))
                .absoluteTransferLimit(MoneyResponse.eur(new BigDecimal("5000.00")))
                .dailyTransferLimit(MoneyResponse.eur(new BigDecimal("2000.00")))
                .status(AccountStatus.ACTIVE)
                .createdAt(account.getCreatedAt())
                .build();

        pageable = PageRequest.of(0, 10);
    }

    // --- listAccounts ---

    @Test
    @DisplayName("listAccounts() - should return all accounts when customerId is null")
    void listAccounts_shouldReturnAllAccounts_whenCustomerIdIsNull() {
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(accountRepository.findAll(pageable)).thenReturn(page);
        when(accountRepository.sumBalance()).thenReturn(new BigDecimal("7500.00"));
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountListResponse result = accountService.listAccounts(null, pageable);

        assertThat(result.getAccounts()).hasSize(1);
        // Combined balance comes from the DB-wide sum, not the single page of accounts.
        assertThat(result.getTotals().getCombinedBalance().getAmount()).isEqualByComparingTo("7500.00");
        verify(accountRepository).findAll(pageable);
        verify(accountRepository, never()).findByCustomerId(any(), any());
    }

    @Test
    @DisplayName("listAccounts() - should filter by customerId when provided")
    void listAccounts_shouldFilterByCustomerId_whenProvided() {
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(accountRepository.findByCustomerId(1L, pageable)).thenReturn(page);
        when(accountRepository.sumBalanceByCustomerId(1L)).thenReturn(new BigDecimal("1500.00"));
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountListResponse result = accountService.listAccounts(1L, pageable);

        assertThat(result.getAccounts()).hasSize(1);
        assertThat(result.getTotals().getCombinedBalance().getAmount()).isEqualByComparingTo("1500.00");
        verify(accountRepository).findByCustomerId(1L, pageable);
        verify(accountRepository, never()).findAll(any(Pageable.class));
    }

    // --- listAccountsOwnedBy ---

    @Test
    @DisplayName("listAccountsOwnedBy() - should return the accounts owned by the given username")
    void listAccountsOwnedBy_shouldReturnAccountsForUsername() {
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(accountRepository.findByCustomerUsername("customer", pageable)).thenReturn(page);
        when(accountRepository.sumBalanceByCustomerUsername("customer")).thenReturn(new BigDecimal("1000.00"));
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountListResponse result = accountService.listAccountsOwnedBy("customer", pageable);

        assertThat(result.getAccounts()).hasSize(1);
        assertThat(result.getTotals().getCombinedBalance().getAmount()).isEqualByComparingTo("1000.00");
        verify(accountRepository).findByCustomerUsername("customer", pageable);
        verify(accountRepository, never()).findAll(any(Pageable.class));
    }

    // --- getAccount ---

    @Test
    @DisplayName("getAccount() - should return account response when account exists")
    void getAccount_shouldReturnResponse_whenAccountExists() {
        when(accountRepository.findById("NL91ABNA0417164300")).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountResponse result = accountService.getAccount("NL91ABNA0417164300");

        assertThat(result.getIban()).isEqualTo("NL91ABNA0417164300");
    }

    @Test
    @DisplayName("getAccount() - should throw EntityNotFoundException when account not found")
    void getAccount_shouldThrow_whenAccountNotFound() {
        when(accountRepository.findById("NL00BANK0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount("NL00BANK0000000000"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    // --- updateAccount ---

    @Test
    @DisplayName("updateAccount() - should update limits and return response")
    void updateAccount_shouldUpdateLimits_whenRequestIsValid() {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal("8000.00"));
        request.setDailyTransferLimit(new BigDecimal("3000.00"));

        when(accountRepository.findById("NL91ABNA0417164300")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountResponse result = accountService.updateAccount("NL91ABNA0417164300", request);

        assertThat(result).isNotNull();
        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("3000.00");
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("updateAccount() - should update only the provided limit and leave the other unchanged")
    void updateAccount_shouldUpdateOnlyProvidedLimit_whenOtherIsNull() {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal("8000.00"));

        when(accountRepository.findById("NL91ABNA0417164300")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        accountService.updateAccount("NL91ABNA0417164300", request);

        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("2000.00");
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("updateAccount() - should close account when status is CLOSED")
    void updateAccount_shouldCloseAccount_whenStatusIsClosed() {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setStatus(AccountStatus.CLOSED);

        when(accountRepository.findById("NL91ABNA0417164300")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        accountService.updateAccount("NL91ABNA0417164300", request);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(account.getClosedAt()).isNotNull();
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("updateAccount() - should throw EntityNotFoundException when account not found")
    void updateAccount_shouldThrow_whenAccountNotFound() {
        AccountUpdateRequest request = new AccountUpdateRequest();

        when(accountRepository.findById("NL00BANK0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateAccount("NL00BANK0000000000", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("updateAccount() - should propagate and not save when the policy rejects the update")
    void updateAccount_shouldPropagateAndNotSave_whenPolicyRejects() {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal("8000.00"));

        when(accountRepository.findById("NL91ABNA0417164300")).thenReturn(Optional.of(account));
        doThrow(new AccountStateException("Cannot update a closed account"))
                .when(accountPolicy).assertCanUpdateLimits(account);

        assertThatThrownBy(() -> accountService.updateAccount("NL91ABNA0417164300", request))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("closed");

        verify(accountRepository, never()).save(any());
    }
}
