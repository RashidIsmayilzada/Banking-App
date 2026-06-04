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
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.policies.AccountPolicy;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.UserRepository;
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
    @Mock private UserRepository userRepository;
    @Mock private AccountMapper accountMapper;
    @Mock private AccountPolicy accountPolicy;

    @InjectMocks private AccountService accountService;

    private User customer;
    private User employee;
    private Account account;
    private AccountResponse accountResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setUsername("customer");
        customer.setRole(Role.CUSTOMER);

        employee = new User();
        employee.setId(2L);
        employee.setUsername("employee");
        employee.setRole(Role.EMPLOYEE);

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
    @DisplayName("listAccounts() - should return only own accounts when user is CUSTOMER")
    void listAccounts_shouldReturnOwnAccounts_whenUserIsCustomer() {
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findByCustomerId(1L, pageable)).thenReturn(page);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountListResponse result = accountService.listAccounts(null, "customer", pageable);

        assertThat(result.getAccounts()).hasSize(1);
        verify(accountRepository).findByCustomerId(1L, pageable);
        verify(accountRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("listAccounts() - should return all accounts when user is EMPLOYEE and no userId filter")
    void listAccounts_shouldReturnAllAccounts_whenEmployeeNoFilter() {
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        when(accountRepository.findAll(pageable)).thenReturn(page);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountListResponse result = accountService.listAccounts(null, "employee", pageable);

        assertThat(result.getAccounts()).hasSize(1);
        verify(accountRepository).findAll(pageable);
        verify(accountRepository, never()).findByCustomerId(any(), any());
    }

    @Test
    @DisplayName("listAccounts() - should filter by userId when user is EMPLOYEE and userId is provided")
    void listAccounts_shouldFilterByUserId_whenEmployeeWithFilter() {
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        when(accountRepository.findByCustomerId(1L, pageable)).thenReturn(page);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountListResponse result = accountService.listAccounts(1L, "employee", pageable);

        assertThat(result.getAccounts()).hasSize(1);
        verify(accountRepository).findByCustomerId(1L, pageable);
    }

    @Test
    @DisplayName("listAccounts() - should ignore userId param and use own id when user is CUSTOMER")
    void listAccounts_shouldIgnoreUserId_whenUserIsCustomer() {
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findByCustomerId(1L, pageable)).thenReturn(page);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountListResponse result = accountService.listAccounts(99L, "customer", pageable);

        assertThat(result.getAccounts()).hasSize(1);
        verify(accountRepository).findByCustomerId(1L, pageable);
        verify(accountRepository, never()).findByCustomerId(eq(99L), any());
        verify(accountRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("listAccounts() - should throw EntityNotFoundException when user not found")
    void listAccounts_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.listAccounts(null, "unknown", pageable))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
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
