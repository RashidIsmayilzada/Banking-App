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
import com.inholland.banking_app.repositories.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String IBAN = "NL91ABNA0417164300";

    @Mock private AccountRepository accountRepository;
    @Mock private AccountMapper accountMapper;

    @InjectMocks private AccountService accountService;

    private Account account;
    private AccountResponse accountResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        User customer = new User();
        customer.setId(1L);
        customer.setUsername("customer");

        account = new Account();
        account.setCustomer(customer);
        account.setIban(IBAN);
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());

        accountResponse = AccountResponse.builder()
                .iban(IBAN)
                .balance(MoneyResponse.eur(new BigDecimal("1000.00")))
                .build();
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void listAccounts_queriesAllAccounts_whenCustomerIdIsNull() {
        when(accountRepository.findAll(pageable)).thenReturn(oneAccountPage());
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountListResponse result = accountService.listAccounts(null, pageable);

        assertEquals(1, result.getAccounts().size());
        verify(accountRepository).findAll(pageable);
        verify(accountRepository, never()).findByCustomerId(any(), any());
    }

    @Test
    void listAccounts_filtersByCustomerId_whenProvided() {
        when(accountRepository.findByCustomerId(1L, pageable)).thenReturn(oneAccountPage());
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        accountService.listAccounts(1L, pageable);

        verify(accountRepository).findByCustomerId(1L, pageable);
        verify(accountRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listAccountsOwnedBy_queriesByUsername() {
        when(accountRepository.findByCustomerUsername("customer", pageable)).thenReturn(oneAccountPage());
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        accountService.listAccountsOwnedBy("customer", pageable);

        verify(accountRepository).findByCustomerUsername("customer", pageable);
    }

    @Test
    void getAccount_returnsMappedResponse_whenAccountExists() {
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        assertEquals(IBAN, accountService.getAccount(IBAN).getIban());
    }

    @Test
    void getAccount_throwsNotFound_whenAccountMissing() {
        when(accountRepository.findById(IBAN)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> accountService.getAccount(IBAN));
    }

    @Test
    void updateAccount_changesBothLimits_whenProvided() {
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));

        accountService.updateAccount(IBAN, limitRequest("8000.00", "3000.00"));

        // BigDecimal equals() is scale-sensitive (8000 != 8000.00); compare by value instead.
        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("3000.00");
        verify(accountRepository).save(account);
    }

    @Test
    void updateAccount_changesOnlyProvidedLimit_andLeavesOtherUntouched() {
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));

        accountService.updateAccount(IBAN, limitRequest("8000.00", null));

        assertThat(account.getAbsoluteTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("2000.00");
    }

    @Test
    void updateAccount_closesAccountAndStampsClosedAt_whenStatusIsClosed() {
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));

        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setStatus(AccountStatus.CLOSED);
        accountService.updateAccount(IBAN, request);

        assertEquals(AccountStatus.CLOSED, account.getStatus());
        assertNotNull(account.getClosedAt());
        verify(accountRepository).save(account);
    }

    @Test
    void updateAccount_throwsNotFound_whenAccountMissing() {
        when(accountRepository.findById(IBAN)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> accountService.updateAccount(IBAN, limitRequest("8000.00", null)));
    }

    @Test
    void updateAccount_throwsAndDoesNotSave_whenAccountIsClosed() {
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));

        AccountStateException thrown = assertThrows(AccountStateException.class,
                () -> accountService.updateAccount(IBAN, limitRequest("8000.00", null)));

        assertTrue(thrown.getMessage().contains("closed"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateAccount_throwsAndDoesNotSave_whenClosingAnAlreadyClosedAccount() {
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(IBAN)).thenReturn(Optional.of(account));

        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setStatus(AccountStatus.CLOSED);

        assertThrows(AccountStateException.class, () -> accountService.updateAccount(IBAN, request));
        verify(accountRepository, never()).save(any());
    }

    private Page<Account> oneAccountPage() {
        return new PageImpl<>(List.of(account), pageable, 1);
    }

    private AccountUpdateRequest limitRequest(String absolute, String daily) {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal(absolute));
        if (daily != null) {
            request.setDailyTransferLimit(new BigDecimal(daily));
        }
        return request;
    }
}
