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
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.policies.TransactionPolicy;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
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

    private static final String FROM_IBAN = "NL91INHO0417164010";
    private static final String TO_IBAN = "NL91INHO0417164300";
    private static final String MISSING_IBAN = "NL91INHO0417164099";

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private DailyTransferUsageRepository dailyTransferUsageRepository;
    @Mock private UserService userService;
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
        when(userService.getByUsername("employee")).thenReturn(employee);
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
        when(userService.getByUsername("customer")).thenReturn(customer);
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);
        when(transactionMapper.toPageDto(emptyPage)).thenReturn(TransactionPageDto.builder().build());

        transactionService.listTransactions(params, "customer");

        assertThat(params.getUserId()).isEqualTo(customer.getId());
    }

    @Test
    @DisplayName("listTransactions() - should throw EntityNotFoundException when user is not found")
    void listTransactions_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userService.getByUsername("ghost")).thenThrow(new EntityNotFoundException("User not found: ghost"));

        assertThatThrownBy(() -> transactionService.listTransactions(new TransactionFilterParams(), "ghost"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    // --- TRANSFER happy path ---

    @Test
    @DisplayName("createTransaction() TRANSFER - should debit source, credit destination, and save transaction")
    void createTransaction_transfer_shouldUpdateBalancesAndSaveTransaction() {
        Account from = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        Account to = makeAccount(TO_IBAN, otherCustomer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        TransactionRequest request = transferRequest(FROM_IBAN, TO_IBAN, 200.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(to));
        when(dailyTransferUsageRepository.findByAccountIbanAndUsageDate(eq(from.getIban()), any())).thenReturn(Optional.empty());
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
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when fromIban is null")
    void createTransaction_transfer_shouldPropagateException_whenFromIbanIsNull() {
        TransactionRequest request = transferRequest(null, TO_IBAN, 100.0);
        when(userService.getByUsername("customer")).thenReturn(customer);
        doThrow(new IllegalArgumentException("fromIban is required for TRANSFER"))
                .when(transactionPolicy).validateTransferFields(request);

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromIban");

        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when destination iban is null")
    void createTransaction_transfer_shouldPropagateException_whenToIbanIsNull() {
        TransactionRequest request = transferRequest(FROM_IBAN, null, 100.0);
        when(userService.getByUsername("customer")).thenReturn(customer);
        doThrow(new IllegalArgumentException("toIban is required for TRANSFER"))
                .when(transactionPolicy).validateTransferFields(request);

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toIban");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should throw EntityNotFoundException when source account does not exist")
    void createTransaction_transfer_shouldThrowEntityNotFoundException_whenSourceAccountNotFound() {
        TransactionRequest request = transferRequest(MISSING_IBAN, TO_IBAN, 100.0);
        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(MISSING_IBAN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Source account");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate ForbiddenException from policy when customer transfers from another customer's account")
    void createTransaction_transfer_shouldPropagateForbiddenException_whenCustomerTransfersFromForeignAccount() {
        Account foreignAccount = makeAccount(FROM_IBAN, otherCustomer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(FROM_IBAN, TO_IBAN, 100.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(foreignAccount));
        doThrow(new ForbiddenException("You can only transfer from your own accounts"))
                .when(transactionPolicy).validateAccountOwnership(eq(foreignAccount), eq(customer), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own accounts");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when source account is inactive")
    void createTransaction_transfer_shouldPropagateException_whenSourceAccountIsInactive() {
        Account inactiveAccount = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), false);
        TransactionRequest request = transferRequest(FROM_IBAN, TO_IBAN, 100.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(inactiveAccount));
        doThrow(new IllegalArgumentException("Source account is not active"))
                .when(transactionPolicy).validateActiveAccount(eq(inactiveAccount), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when source account is SAVINGS")
    void createTransaction_transfer_shouldPropagateException_whenSourceAccountIsSavings() {
        Account savingsAccount = makeAccount(FROM_IBAN, customer, AccountType.SAVINGS,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(FROM_IBAN, TO_IBAN, 100.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(savingsAccount));
        doThrow(new IllegalArgumentException("Transfers can only be made from a checking account"))
                .when(transactionPolicy).validateCheckingAccount(savingsAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checking account");
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when balance is insufficient")
    void createTransaction_transfer_shouldPropagateException_whenBalanceIsInsufficient() {
        Account from = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        Account to = makeAccount(TO_IBAN, otherCustomer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(FROM_IBAN, TO_IBAN, 150.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(to));
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
        Account from = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("300.00"), true);
        Account to = makeAccount(TO_IBAN, otherCustomer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(FROM_IBAN, TO_IBAN, 200.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(to));
        doThrow(new IllegalArgumentException("Daily transfer limit exceeded"))
                .when(transactionPolicy).checkDailyLimit(eq(from), any(BigDecimal.class));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Daily transfer limit");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should throw EntityNotFoundException when destination IBAN does not exist")
    void createTransaction_transfer_shouldThrowEntityNotFoundException_whenDestinationIbanNotFound() {
        Account from = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(FROM_IBAN, TO_IBAN, 100.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(TO_IBAN);
    }

    @Test
    @DisplayName("createTransaction() TRANSFER - should propagate IllegalArgumentException from policy when destination account is inactive")
    void createTransaction_transfer_shouldPropagateException_whenDestinationAccountIsInactive() {
        Account from = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        Account inactiveTo = makeAccount(TO_IBAN, otherCustomer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), false);
        TransactionRequest request = transferRequest(FROM_IBAN, TO_IBAN, 100.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(inactiveTo));
        // Source account is validated first (passes), destination second (throws)
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
        Account from = makeAccount(FROM_IBAN, employee, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        Account to = makeAccount(TO_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = transferRequest(FROM_IBAN, TO_IBAN, 100.0);

        when(userService.getByUsername("employee")).thenReturn(employee);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(from));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(to));
        when(dailyTransferUsageRepository.findByAccountIbanAndUsageDate(any(), any())).thenReturn(Optional.empty());
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
        Account account = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = depositRequest(FROM_IBAN, 200.0);

        when(userService.getByUsername("employee")).thenReturn(employee);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(account));
        when(dailyTransferUsageRepository.findByAccountIbanAndUsageDate(eq(account.getIban()), any())).thenReturn(Optional.empty());
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
    @DisplayName("createTransaction() DEPOSIT - should propagate IllegalArgumentException from policy when iban is null")
    void createTransaction_deposit_shouldPropagateException_whenIbanIsNull() {
        TransactionRequest request = depositRequest(null, 100.0);
        when(userService.getByUsername("employee")).thenReturn(employee);
        doThrow(new IllegalArgumentException("iban is required for DEPOSIT"))
                .when(transactionPolicy).requireIban(eq(request), eq("DEPOSIT"));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "employee"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iban");
    }

    @Test
    @DisplayName("createTransaction() DEPOSIT - should throw EntityNotFoundException when account does not exist")
    void createTransaction_deposit_shouldThrowEntityNotFoundException_whenAccountNotFound() {
        TransactionRequest request = depositRequest(MISSING_IBAN, 100.0);
        when(userService.getByUsername("employee")).thenReturn(employee);
        when(accountRepository.findById(MISSING_IBAN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "employee"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("createTransaction() DEPOSIT - should propagate IllegalArgumentException from policy when account is inactive")
    void createTransaction_deposit_shouldPropagateException_whenAccountIsInactive() {
        Account inactiveAccount = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), false);
        TransactionRequest request = depositRequest(FROM_IBAN, 100.0);

        when(userService.getByUsername("employee")).thenReturn(employee);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(inactiveAccount));
        doThrow(new IllegalArgumentException("Account is not active"))
                .when(transactionPolicy).validateActiveAccount(eq(inactiveAccount), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "employee"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("createTransaction() DEPOSIT - should propagate IllegalArgumentException from policy when daily limit is exceeded")
    void createTransaction_deposit_shouldPropagateException_whenDailyLimitIsExceeded() {
        Account account = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("300.00"), true);
        TransactionRequest request = depositRequest(FROM_IBAN, 200.0);

        when(userService.getByUsername("employee")).thenReturn(employee);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(account));
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
        Account account = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = withdrawalRequest(FROM_IBAN, 100.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(account));
        when(dailyTransferUsageRepository.findByAccountIbanAndUsageDate(eq(account.getIban()), any())).thenReturn(Optional.empty());
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
    @DisplayName("createTransaction() WITHDRAWAL - should propagate IllegalArgumentException from policy when iban is null")
    void createTransaction_withdrawal_shouldPropagateException_whenIbanIsNull() {
        TransactionRequest request = withdrawalRequest(null, 100.0);
        when(userService.getByUsername("customer")).thenReturn(customer);
        doThrow(new IllegalArgumentException("iban is required for WITHDRAWAL"))
                .when(transactionPolicy).requireIban(eq(request), eq("WITHDRAWAL"));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iban");
    }

    @Test
    @DisplayName("createTransaction() WITHDRAWAL - should propagate ForbiddenException from policy when customer withdraws from another customer's account")
    void createTransaction_withdrawal_shouldPropagateForbiddenException_whenCustomerWithdrawsFromForeignAccount() {
        Account foreignAccount = makeAccount(FROM_IBAN, otherCustomer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = withdrawalRequest(FROM_IBAN, 100.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(foreignAccount));
        doThrow(new ForbiddenException("You can only withdraw from your own accounts"))
                .when(transactionPolicy).validateAccountOwnership(eq(foreignAccount), eq(customer), anyString());

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own accounts");
    }

    @Test
    @DisplayName("createTransaction() WITHDRAWAL - should propagate IllegalArgumentException from policy when balance is insufficient")
    void createTransaction_withdrawal_shouldPropagateException_whenBalanceIsInsufficient() {
        Account account = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);
        TransactionRequest request = withdrawalRequest(FROM_IBAN, 150.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(account));
        doThrow(new IllegalArgumentException("Insufficient funds"))
                .when(transactionPolicy).checkBalance(eq(account), any(BigDecimal.class));

        assertThatThrownBy(() -> transactionService.createTransaction(request, "customer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("createTransaction() WITHDRAWAL - should propagate IllegalArgumentException from policy when daily limit is exceeded")
    void createTransaction_withdrawal_shouldPropagateException_whenDailyLimitIsExceeded() {
        Account account = makeAccount(FROM_IBAN, customer, AccountType.CHECKING,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("300.00"), true);
        TransactionRequest request = withdrawalRequest(FROM_IBAN, 200.0);

        when(userService.getByUsername("customer")).thenReturn(customer);
        when(accountRepository.findById(FROM_IBAN)).thenReturn(Optional.of(account));
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

    private Account makeAccount(String iban, User owner, AccountType type, BigDecimal balance,
                                BigDecimal absLimit, BigDecimal dailyLimit, boolean active) {
        Account account = new Account();
        account.setIban(iban);
        account.setCustomer(owner);
        account.setAccountType(type);
        account.setBalance(balance);
        account.setAbsoluteTransferLimit(absLimit);
        account.setDailyTransferLimit(dailyLimit);
        account.setStatus(active ? AccountStatus.ACTIVE : AccountStatus.CLOSED);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }

    private TransactionRequest transferRequest(String fromIban, String toIban, double amount) {
        TransactionRequest r = new TransactionRequest();
        r.setType(TransactionType.TRANSFER);
        r.setFromIban(fromIban);
        r.setToIban(toIban);
        r.setAmount(amount);
        r.setDescription("Test transfer");
        return r;
    }

    private TransactionRequest depositRequest(String iban, double amount) {
        TransactionRequest r = new TransactionRequest();
        r.setType(TransactionType.DEPOSIT);
        r.setIban(iban);
        r.setAmount(amount);
        r.setDescription("Test deposit");
        return r;
    }

    private TransactionRequest withdrawalRequest(String iban, double amount) {
        TransactionRequest r = new TransactionRequest();
        r.setType(TransactionType.WITHDRAWAL);
        r.setIban(iban);
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