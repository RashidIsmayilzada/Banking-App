package com.inholland.banking_app.policies;

import com.inholland.banking_app.dtos.TransactionRequest;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.DailyTransferUsage;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionPolicyTest {

    @Mock private DailyTransferUsageRepository dailyTransferUsageRepository;
    @InjectMocks private TransactionPolicy transactionPolicy;

    private User customer;
    private User otherCustomer;
    private User employee;

    @BeforeEach
    void setUp() {
        customer = makeUser(1L, Role.CUSTOMER);
        otherCustomer = makeUser(2L, Role.CUSTOMER);
        employee = makeUser(3L, Role.EMPLOYEE);
    }

    // --- validateTransferFields ---

    @Test
    @DisplayName("validateTransferFields() - should throw when fromAccountId is null")
    void validateTransferFields_shouldThrow_whenFromAccountIdIsNull() {
        TransactionRequest request = transferRequest(null, 2L, null, 100.0);

        assertThatThrownBy(() -> transactionPolicy.validateTransferFields(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromAccountId");
    }

    @Test
    @DisplayName("validateTransferFields() - should throw when both toAccountId and toIban are null")
    void validateTransferFields_shouldThrow_whenBothDestinationFieldsAreNull() {
        TransactionRequest request = transferRequest(1L, null, null, 100.0);

        assertThatThrownBy(() -> transactionPolicy.validateTransferFields(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toAccountId or toIban");
    }

    @Test
    @DisplayName("validateTransferFields() - should pass when fromAccountId and toAccountId are set")
    void validateTransferFields_shouldPass_whenFromAndToAccountIdSet() {
        TransactionRequest request = transferRequest(1L, 2L, null, 100.0);

        assertThatCode(() -> transactionPolicy.validateTransferFields(request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateTransferFields() - should pass when fromAccountId and toIban are set")
    void validateTransferFields_shouldPass_whenFromAccountIdAndToIbanSet() {
        TransactionRequest request = transferRequest(1L, null, "NL91INHO0417164300", 100.0);

        assertThatCode(() -> transactionPolicy.validateTransferFields(request))
                .doesNotThrowAnyException();
    }

    // --- requireAccountId ---

    @Test
    @DisplayName("requireAccountId() - should throw when accountId is null")
    void requireAccountId_shouldThrow_whenAccountIdIsNull() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountId(null);

        assertThatThrownBy(() -> transactionPolicy.requireAccountId(request, "DEPOSIT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId")
                .hasMessageContaining("DEPOSIT");
    }

    @Test
    @DisplayName("requireAccountId() - should include the operation type in the message")
    void requireAccountId_shouldIncludeType_inExceptionMessage() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountId(null);

        assertThatThrownBy(() -> transactionPolicy.requireAccountId(request, "WITHDRAWAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WITHDRAWAL");
    }

    @Test
    @DisplayName("requireAccountId() - should pass when accountId is set")
    void requireAccountId_shouldPass_whenAccountIdIsSet() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountId(1L);

        assertThatCode(() -> transactionPolicy.requireAccountId(request, "WITHDRAWAL"))
                .doesNotThrowAnyException();
    }

    // --- validateActiveAccount ---

    @Test
    @DisplayName("validateActiveAccount() - should throw with provided message when account is inactive")
    void validateActiveAccount_shouldThrowWithProvidedMessage_whenAccountIsInactive() {
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), false);

        assertThatThrownBy(() -> transactionPolicy.validateActiveAccount(account, "Source account is not active"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source account is not active");
    }

    @Test
    @DisplayName("validateActiveAccount() - should pass when account is active")
    void validateActiveAccount_shouldPass_whenAccountIsActive() {
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        assertThatCode(() -> transactionPolicy.validateActiveAccount(account, "Account is not active"))
                .doesNotThrowAnyException();
    }

    // --- validateCheckingAccount ---

    @Test
    @DisplayName("validateCheckingAccount() - should throw when account type is SAVINGS")
    void validateCheckingAccount_shouldThrow_whenAccountIsSavings() {
        Account account = makeAccount(1L, customer, AccountType.SAVINGS,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        assertThatThrownBy(() -> transactionPolicy.validateCheckingAccount(account))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checking account");
    }

    @Test
    @DisplayName("validateCheckingAccount() - should pass when account type is CHECKING")
    void validateCheckingAccount_shouldPass_whenAccountIsChecking() {
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        assertThatCode(() -> transactionPolicy.validateCheckingAccount(account))
                .doesNotThrowAnyException();
    }

    // --- validateAccountOwnership ---

    @Test
    @DisplayName("validateAccountOwnership() - should throw ForbiddenException when customer accesses another customer's account")
    void validateAccountOwnership_shouldThrowForbidden_whenCustomerAccessesForeignAccount() {
        Account account = makeAccount(1L, otherCustomer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        assertThatThrownBy(() -> transactionPolicy.validateAccountOwnership(account, customer, "Access denied"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("validateAccountOwnership() - should pass when customer accesses their own account")
    void validateAccountOwnership_shouldPass_whenCustomerAccessesOwnAccount() {
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        assertThatCode(() -> transactionPolicy.validateAccountOwnership(account, customer, "Access denied"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateAccountOwnership() - should pass when employee accesses any customer's account")
    void validateAccountOwnership_shouldPass_whenEmployeeAccessesAnyAccount() {
        Account account = makeAccount(1L, otherCustomer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        assertThatCode(() -> transactionPolicy.validateAccountOwnership(account, employee, "Access denied"))
                .doesNotThrowAnyException();
    }

    // --- checkBalance ---

    @Test
    @DisplayName("checkBalance() - should throw when balance minus amount falls below absolute transfer limit")
    void checkBalance_shouldThrow_whenBalanceIsInsufficient() {
        // balance(100) - amount(150) = -50, below absoluteTransferLimit(0)
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        assertThatThrownBy(() -> transactionPolicy.checkBalance(account, new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("checkBalance() - should pass when balance minus amount exactly equals absolute transfer limit")
    void checkBalance_shouldPass_whenBalanceEqualsAbsoluteLimit() {
        // balance(100) - amount(100) = 0 == absoluteTransferLimit(0)
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        assertThatCode(() -> transactionPolicy.checkBalance(account, new BigDecimal("100.00")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkBalance() - should pass when balance is well above the amount")
    void checkBalance_shouldPass_whenBalanceIsSufficient() {
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), true);

        assertThatCode(() -> transactionPolicy.checkBalance(account, new BigDecimal("200.00")))
                .doesNotThrowAnyException();
    }

    // --- checkDailyLimit ---

    @Test
    @DisplayName("checkDailyLimit() - should throw when used plus requested amount exceeds daily limit")
    void checkDailyLimit_shouldThrow_whenLimitIsExceeded() {
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("300.00"), true);

        DailyTransferUsage existing = new DailyTransferUsage();
        existing.setTotalOutgoingAmount(new BigDecimal("200.00"));
        when(dailyTransferUsageRepository.findByAccountIdAndUsageDate(eq(account.getId()), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionPolicy.checkDailyLimit(account, new BigDecimal("200.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Daily transfer limit");
    }

    @Test
    @DisplayName("checkDailyLimit() - should pass when used plus requested amount exactly equals daily limit")
    void checkDailyLimit_shouldPass_whenAmountEqualsRemainingLimit() {
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("300.00"), true);

        DailyTransferUsage existing = new DailyTransferUsage();
        existing.setTotalOutgoingAmount(new BigDecimal("100.00"));
        when(dailyTransferUsageRepository.findByAccountIdAndUsageDate(eq(account.getId()), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        assertThatCode(() -> transactionPolicy.checkDailyLimit(account, new BigDecimal("200.00")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkDailyLimit() - should pass when no daily usage record exists yet")
    void checkDailyLimit_shouldPass_whenNoUsageRecordExists() {
        Account account = makeAccount(1L, customer, AccountType.CHECKING,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("300.00"), true);

        when(dailyTransferUsageRepository.findByAccountIdAndUsageDate(eq(account.getId()), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThatCode(() -> transactionPolicy.checkDailyLimit(account, new BigDecimal("200.00")))
                .doesNotThrowAnyException();
    }

    // --- helpers ---

    private User makeUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(role.name().toLowerCase() + id);
        user.setEmail(role.name().toLowerCase() + id + "@bank.com");
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
        account.setStatus(active ? AccountStatus.ACTIVE : AccountStatus.CLOSED);
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
}
