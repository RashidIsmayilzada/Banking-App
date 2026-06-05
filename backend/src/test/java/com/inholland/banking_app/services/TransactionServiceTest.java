package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.MoneyDto;
import com.inholland.banking_app.dtos.TransactionFilterParams;
import com.inholland.banking_app.dtos.TransactionPageDto;
import com.inholland.banking_app.dtos.TransactionRequest;
import com.inholland.banking_app.dtos.TransactionResultDto;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.mappers.TransactionMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.policies.TransactionPolicy;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private DailyTransferUsageRepository dailyTransferUsageRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionMapper transactionMapper;
    @Mock private TransactionPolicy transactionPolicy;

    @InjectMocks private TransactionService transactionService;

    private User employee;
    private User customer;
    private User otherCustomer;

    @BeforeEach
    void setUp() {
        employee = makeUser(1L, Role.EMPLOYEE);
        customer = makeUser(2L, Role.CUSTOMER);
        otherCustomer = makeUser(3L, Role.CUSTOMER);
    }

    // --- listTransactions ---

    @Test
    @DisplayName("listTransactions() - should not override userId for EMPLOYEE")
    void listTransactions_shouldNotForceUserId_whenCallerIsEmployee() {
        TransactionFilterParams params = new TransactionFilterParams();
        params.setSize(10);
        params.setUserId(99L);

        Page<Transaction> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);
        when(transactionMapper.toPageDto(emptyPage)).thenReturn(TransactionPageDto.builder().build());

        transactionService.listTransactions(params, "employee");

        assertThat(params.getUserId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("listTransactions() - should force userId to caller's own id when caller is CUSTOMER")
    void listTransactions_shouldForceUserId_whenCallerIsCustomer() {
        TransactionFilterParams params = new TransactionFilterParams();
        params.setSize(10);
        params.setUserId(99L);

        Page<Transaction> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);
        when(transactionMapper.toPageDto(emptyPage)).thenReturn(TransactionPageDto.builder().build());

        transactionService.listTransactions(params, "customer");

        assertThat(params.getUserId()).isEqualTo(customer.getId());
    }

    @Test
    @DisplayName("listTransactions() - should throw EntityNotFoundException when user is not found")
    void listTransactions_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.listTransactions(new TransactionFilterParams(), "ghost"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    // --- TRANSFER happy path ---

    @Test
    @DisplayName("createTransaction() TRANSFER - should debit source, credit destination, and save transaction")
    void createTransaction_transfer_shouldUpdateBalancesAndSaveTransaction() {
        Account from = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        Account to = makeAccount(20L, otherCustomer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        TransactionRequest request = transferRequest(10L, null, "NL91INHO0417164300", 200.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban("NL91INHO0417164300")).thenReturn(Optional.of(to));
        when(dailyTransferUsageRepository.findByAccountAndUsageDate(eq(from), any())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toTransferResult(any(Transaction.class), eq(from), eq(to)))
                .thenReturn(dummyTransferResult());

        TransactionResultDto result = transactionService.createTransaction(request, "customer");

        assertThat(result).isNotNull();
        assertThat(from.getBalance()).isEqualByComparingTo("300.00");
        assertThat(to.getBalance()).isEqualByComparingTo("300.00");
        verify(transactionRepository).save(any(Transaction.class));
    }

    // --- TRANSFER validation failures ---

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when fromAccountId is null")
    void createTransaction_transfer_shouldPropagateException_whenFromAccountIdIsNull() {
        TransactionRequest request = transferRequest(null, 2L, null, 100.0);
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        doThrow(new IllegalArgumentException("fromAccountId is required for TRANSFER"))
                .when(transactionPolicy).validateTransferFields(request);

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromAccountId");

        verify(accountRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when both destination fields are null")
    void createTransaction_transfer_shouldPropagateException_whenBothDestinationFieldsAreNull() {
        TransactionRequest request = transferRequest(1L, null, null, 100.0);
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        doThrow(new IllegalArgumentException("toAccountId or toIban is required for TRANSFER"))
                .when(transactionPolicy).validateTransferFields(request);

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toAccountId or toIban");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should throw EntityNotFoundException when source account does not exist")
    void createTransaction_transfer_shouldThrowEntityNotFoundException_whenSourceAccountNotFound() {
        TransactionRequest request = transferRequest(99L, null, "NL91INHO0417164300", 100.0);
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Source account");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate ForbiddenException from policy when customer transfers from another customer's account")
    void createTransaction_transfer_shouldPropagateForbiddenException_whenCustomerTransfersFromForeignAccount() {
        Account foreignAccount = makeAccount(10L, otherCustomer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(10L, null, "NL91INHO0417164300", 100.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(foreignAccount));
        doThrow(new ForbiddenException("You can only transfer from your own accounts"))
                .when(transactionPolicy).validateAccountOwnership(eq(foreignAccount), eq(customer), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own accounts");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when source account is inactive")
    void createTransaction_transfer_shouldPropagateException_whenSourceAccountIsInactive() {
        Account inactiveAccount = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), false);
        TransactionRequest request = transferRequest(10L, null, "NL91INHO0417164300", 100.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(inactiveAccount));
        doThrow(new IllegalArgumentException("Source account is not active"))
                .when(transactionPolicy).validateActiveAccount(eq(inactiveAccount), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when source account is SAVINGS")
    void createTransaction_transfer_shouldPropagateException_whenSourceAccountIsSavings() {
        Account savingsAccount = makeAccount(10L, customer, AccountType.SAVINGS,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(10L, null, "NL91INHO0417164300", 100.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(savingsAccount));
        doThrow(new IllegalArgumentException("Transfers can only be made from a checking account"))
                .when(transactionPolicy).validateCheckingAccount(savingsAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checking account");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when balance is insufficient")
    void createTransaction_transfer_shouldPropagateException_whenBalanceIsInsufficient() {
        Account from = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        Account to = makeAccount(20L, otherCustomer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(10L, null, "NL91INHO0417164300", 150.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban("NL91INHO0417164300")).thenReturn(Optional.of(to));
        doThrow(new IllegalArgumentException("Insufficient funds"))
                .when(transactionPolicy).checkBalance(eq(from), any(BigDecimal.class));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient funds");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when daily transfer limit is exceeded")
    void createTransaction_transfer_shouldPropagateException_whenDailyLimitIsExceeded() {
        Account from = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("300.00"), true);
        Account to = makeAccount(20L, otherCustomer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(10L, null, "NL91INHO0417164300", 200.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban("NL91INHO0417164300")).thenReturn(Optional.of(to));
        doThrow(new IllegalArgumentException("Daily transfer limit exceeded"))
                .when(transactionPolicy).checkDailyLimit(eq(from), any(BigDecimal.class));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Daily transfer limit");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate ForbiddenException from policy when customer uses toAccountId for another customer's account")
    void createTransaction_transfer_shouldPropagateForbiddenException_whenCustomerUsesToAccountIdForForeignAccount() {
        Account from = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        Account foreignDestination = makeAccount(20L, otherCustomer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(10L, 20L, null, 100.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(foreignDestination));
        doNothing()
                .doThrow(new ForbiddenException("Use toIban to transfer to another customer's account"))
                .when(transactionPolicy).validateAccountOwnership(any(Account.class), any(User.class), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("toIban");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should throw EntityNotFoundException when destination account id does not exist")
    void createTransaction_transfer_shouldThrowEntityNotFoundException_whenDestinationAccountIdNotFound() {
        Account from = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(10L, 99L, null, 100.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Destination account");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should throw EntityNotFoundException when destination IBAN does not exist")
    void createTransaction_transfer_shouldThrowEntityNotFoundException_whenDestinationIbanNotFound() {
        Account from = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(10L, null, "NL91INHO0417164300", 100.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban("NL91INHO0417164300")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("NL91INHO0417164300");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when destination account is inactive")
    void createTransaction_transfer_shouldPropagateException_whenDestinationAccountIsInactive() {
        Account from = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        Account inactiveTo = makeAccount(20L, otherCustomer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), false);
        TransactionRequest request = transferRequest(10L, null, "NL91INHO0417164300", 100.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban("NL91INHO0417164300")).thenReturn(Optional.of(inactiveTo));
        doNothing()
                .doThrow(new IllegalArgumentException("Destination account is not active"))
                .when(transactionPolicy).validateActiveAccount(any(Account.class), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Destination account is not active");
    }

    // --- TRANSFER channel ---

    @Test
    @DisplayName("createTransaction() TRANSFER - employee channel is EMPLOYEE, customer channel is WEB")
    void createTransaction_transfer_shouldUseCorrectChannel_basedOnCallerRole() {
        Account from = makeAccount(10L, employee, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        Account to = makeAccount(20L, customer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(10L, null, "NL91INHO0417164300", 100.0);

        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban("NL91INHO0417164300")).thenReturn(Optional.of(to));
        when(dailyTransferUsageRepository.findByAccountAndUsageDate(any(), any())).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction saved = inv.getArgument(0);
            assertThat(saved.getChannel().name()).isEqualTo("EMPLOYEE");
            return saved;
        });
        when(transactionMapper.toTransferResult(any(Transaction.class), eq(from), eq(to)))
                .thenReturn(dummyTransferResult());

        transactionService.createTransaction(request, "employee");

        verify(transactionRepository).save(any(Transaction.class));
    }

    // --- DEPOSIT happy path ---

    @Test
    @DisplayName("createTransaction() DEPOSIT - should credit account and save transaction")
    void createTransaction_deposit_shouldCreditAccountAndSaveTransaction() {
        Account account = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = depositRequest(10L, 200.0);

        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(dailyTransferUsageRepository.findByAccountAndUsageDate(eq(account), any())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toSingleAccountResult(any(Transaction.class), eq(account)))
                .thenReturn(dummySingleResult());

        TransactionResultDto result = transactionService.createTransaction(request, "employee");

        assertThat(result).isNotNull();
        assertThat(account.getBalance()).isEqualByComparingTo("700.00");
        verify(transactionRepository).save(any(Transaction.class));
    }

    // --- DEPOSIT validation failures ---

    @Test
    @DisplayName("createTransaction() DEPOSIT - should propagate IllegalArgumentException from policy when accountId is null")
    void createTransaction_deposit_shouldPropagateException_whenAccountIdIsNull() {
        TransactionRequest request = depositRequest(null, 100.0);
        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        doThrow(new IllegalArgumentException("accountId is required for DEPOSIT"))
                .when(transactionPolicy).requireAccountId(eq(request), eq("DEPOSIT"));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "employee"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");
    }

    @Test
    @DisplayName("createTransaction() DEPOSIT - should throw EntityNotFoundException when account does not exist")
    void createTransaction_deposit_shouldThrowEntityNotFoundException_whenAccountNotFound() {
        TransactionRequest request = depositRequest(99L, 100.0);
        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "employee"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("createTransaction() DEPOSIT - should propagate IllegalArgumentException from policy when account is inactive")
    void createTransaction_deposit_shouldPropagateException_whenAccountIsInactive() {
        Account inactiveAccount = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), false);
        TransactionRequest request = depositRequest(10L, 100.0);

        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(inactiveAccount));
        doThrow(new IllegalArgumentException("Account is not active"))
                .when(transactionPolicy).validateActiveAccount(eq(inactiveAccount), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "employee"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("createTransaction() DEPOSIT - should propagate IllegalArgumentException from policy when daily limit is exceeded")
    void createTransaction_deposit_shouldPropagateException_whenDailyLimitIsExceeded() {
        Account account = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("300.00"), true);
        TransactionRequest request = depositRequest(10L, 200.0);

        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        doThrow(new IllegalArgumentException("Daily transfer limit exceeded"))
                .when(transactionPolicy).checkDailyLimit(eq(account), any(BigDecimal.class));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "employee"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Daily transfer limit");
    }

    // --- WITHDRAWAL happy path ---

    @Test
    @DisplayName("createTransaction() WITHDRAWAL - should debit account and save transaction")
    void createTransaction_withdrawal_shouldDebitAccountAndSaveTransaction() {
        Account account = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = withdrawalRequest(10L, 100.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(dailyTransferUsageRepository.findByAccountAndUsageDate(eq(account), any())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toSingleAccountResult(any(Transaction.class), eq(account)))
                .thenReturn(dummySingleResult());

        TransactionResultDto result = transactionService.createTransaction(request, "customer");

        assertThat(result).isNotNull();
        assertThat(account.getBalance()).isEqualByComparingTo("400.00");
        verify(transactionRepository).save(any(Transaction.class));
    }

    // --- WITHDRAWAL validation failures ---

    @Test
    @DisplayName("createTransaction() WITHDRAWAL - should propagate IllegalArgumentException from policy when accountId is null")
    void createTransaction_withdrawal_shouldPropagateException_whenAccountIdIsNull() {
        TransactionRequest request = withdrawalRequest(null, 100.0);
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        doThrow(new IllegalArgumentException("accountId is required for WITHDRAWAL"))
                .when(transactionPolicy).requireAccountId(eq(request), eq("WITHDRAWAL"));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");
    }

    @Test
    @DisplayName("createTransaction() WITHDRAWAL - should propagate ForbiddenException from policy when customer withdraws from another customer's account")
    void createTransaction_withdrawal_shouldPropagateForbiddenException_whenCustomerWithdrawsFromForeignAccount() {
        Account foreignAccount = makeAccount(10L, otherCustomer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = withdrawalRequest(10L, 100.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(foreignAccount));
        doThrow(new ForbiddenException("You can only withdraw from your own accounts"))
                .when(transactionPolicy).validateAccountOwnership(eq(foreignAccount), eq(customer), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own accounts");
    }

    @Test
    @DisplayName("createTransaction() WITHDRAWAL - should propagate IllegalArgumentException from policy when balance is insufficient")
    void createTransaction_withdrawal_shouldPropagateException_whenBalanceIsInsufficient() {
        Account account = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = withdrawalRequest(10L, 150.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        doThrow(new IllegalArgumentException("Insufficient funds"))
                .when(transactionPolicy).checkBalance(eq(account), any(BigDecimal.class));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("createTransaction() WITHDRAWAL - should propagate IllegalArgumentException from policy when daily limit is exceeded")
    void createTransaction_withdrawal_shouldPropagateException_whenDailyLimitIsExceeded() {
        Account account = makeAccount(10L, customer, AccountType.CHECKING,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("300.00"), true);
        TransactionRequest request = withdrawalRequest(10L, 200.0);

        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        doThrow(new IllegalArgumentException("Daily transfer limit exceeded"))
                .when(transactionPolicy).checkDailyLimit(eq(account), any(BigDecimal.class));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Daily transfer limit");
    }

    // --- helpers ---

    private User makeUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(role.name().toLowerCase());
        user.setEmail(role.name().toLowerCase() + "@bank.com");
        user.setPasswordHash("hashed");
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private Account makeAccount(Long id, User owner, AccountType type, BigDecimal balance,
                                BigDecimal absLimit, BigDecimal dailyLimit, boolean active) {
        Account account = new Account();
        account.setId(id);
        account.setCustomer(owner);
        account.setIban("NL91INHO0417164" + String.format("%03d", id));
        account.setAccountType(type);
        account.setBalance(balance);
        account.setAbsoluteTransferLimit(absLimit);
        account.setDailyTransferLimit(dailyLimit);
        account.setActive(active);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }

    private TransactionRequest transferRequest(Long fromId, Long toId, String toIban, double amount) {
        TransactionRequest r = new TransactionRequest();
        r.setType(TransactionType.TRANSFER);
        r.setFromAccountId(fromId);
        r.setToAccountId(toId);
        r.setToIban(toIban);
        r.setAmount(amount);
        r.setDescription("Test transfer");
        return r;
    }

    private TransactionRequest depositRequest(Long accountId, double amount) {
        TransactionRequest r = new TransactionRequest();
        r.setType(TransactionType.DEPOSIT);
        r.setAccountId(accountId);
        r.setAmount(amount);
        r.setDescription("Test deposit");
        return r;
    }

    private TransactionRequest withdrawalRequest(Long accountId, double amount) {
        TransactionRequest r = new TransactionRequest();
        r.setType(TransactionType.WITHDRAWAL);
        r.setAccountId(accountId);
        r.setAmount(amount);
        r.setDescription("Test withdrawal");
        return r;
    }

    private TransactionResultDto dummyTransferResult() {
        MoneyDto eur = MoneyDto.builder().amount(0.0).currency("EUR").build();
        return TransactionResultDto.builder().sourceBalance(eur).destinationBalance(eur).build();
    }

    private TransactionResultDto dummySingleResult() {
        return TransactionResultDto.builder()
                .sourceBalance(MoneyDto.builder().amount(0.0).currency("EUR").build())
                .build();
    }
}
