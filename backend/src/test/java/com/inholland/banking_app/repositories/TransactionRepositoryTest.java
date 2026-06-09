package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private Account fromAccount;
    private Account otherAccount;
    private User user;

    private final LocalDate TODAY = LocalDate.of(2026, 6, 9);

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("user@bank.com");
        user.setUsername("user");
        user.setPasswordHash("hashed");
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        fromAccount = buildAccount(user, "NL91ABNA0417164300");
        otherAccount = buildAccount(user, "NL91ABNA0417164301");
        Account toAccount = buildAccount(user, "NL91ABNA0417164302");

        fromAccount = accountRepository.save(fromAccount);
        otherAccount = accountRepository.save(otherAccount);
        toAccount = accountRepository.save(toAccount);
    }

    @Test
    @DisplayName("sumOutgoingAmountByAccountIdAndDate() - should return correct sum for transfers on the given day")
    void sumOutgoing_shouldReturnCorrectSum() {
        saveTransaction(fromAccount, new BigDecimal("100.00"), TransactionType.TRANSFER, TODAY.atTime(10, 0));
        saveTransaction(fromAccount, new BigDecimal("200.00"), TransactionType.TRANSFER, TODAY.atTime(14, 0));

        BigDecimal result = transactionRepository.sumOutgoingAmountByAccountIdAndDate(
                fromAccount.getId(), TODAY, TODAY.plusDays(1));

        assertThat(result).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("sumOutgoingAmountByAccountIdAndDate() - should return 0 when no transfers exist")
    void sumOutgoing_shouldReturnZero_whenNoTransactions() {
        BigDecimal result = transactionRepository.sumOutgoingAmountByAccountIdAndDate(
                fromAccount.getId(), TODAY, TODAY.plusDays(1));

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("sumOutgoingAmountByAccountIdAndDate() - should exclude non-TRANSFER transactions")
    void sumOutgoing_shouldExcludeDepositsAndWithdrawals() {
        saveTransaction(fromAccount, new BigDecimal("500.00"), TransactionType.DEPOSIT, TODAY.atTime(9, 0));
        saveTransaction(fromAccount, new BigDecimal("300.00"), TransactionType.WITHDRAWAL, TODAY.atTime(11, 0));
        saveTransaction(fromAccount, new BigDecimal("100.00"), TransactionType.TRANSFER, TODAY.atTime(12, 0));

        BigDecimal result = transactionRepository.sumOutgoingAmountByAccountIdAndDate(
                fromAccount.getId(), TODAY, TODAY.plusDays(1));

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("sumOutgoingAmountByAccountIdAndDate() - should exclude transactions from other accounts")
    void sumOutgoing_shouldExcludeOtherAccounts() {
        saveTransaction(otherAccount, new BigDecimal("999.00"), TransactionType.TRANSFER, TODAY.atTime(10, 0));
        saveTransaction(fromAccount, new BigDecimal("50.00"), TransactionType.TRANSFER, TODAY.atTime(11, 0));

        BigDecimal result = transactionRepository.sumOutgoingAmountByAccountIdAndDate(
                fromAccount.getId(), TODAY, TODAY.plusDays(1));

        assertThat(result).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("sumOutgoingAmountByAccountIdAndDate() - should exclude transactions outside the date range")
    void sumOutgoing_shouldExcludeTransactionsOutsideDateRange() {
        saveTransaction(fromAccount, new BigDecimal("400.00"), TransactionType.TRANSFER, TODAY.minusDays(1).atTime(23, 59));
        saveTransaction(fromAccount, new BigDecimal("100.00"), TransactionType.TRANSFER, TODAY.atTime(10, 0));

        BigDecimal result = transactionRepository.sumOutgoingAmountByAccountIdAndDate(
                fromAccount.getId(), TODAY, TODAY.plusDays(1));

        assertThat(result).isEqualByComparingTo("100.00");
    }

    private void saveTransaction(Account from, BigDecimal amount, TransactionType type, LocalDateTime createdAt) {
        Transaction t = new Transaction();
        t.setFromAccount(from);
        t.setToAccount(otherAccount);
        t.setAmount(amount);
        t.setTransactionType(type);
        t.setCurrency("EUR");
        t.setChannel(Channel.WEB);
        t.setInitiatedBy(user);
        t.setCreatedAt(createdAt);
        t.setDescription("test transaction");
        transactionRepository.save(t);
    }

    private Account buildAccount(User customer, String iban) {
        Account account = new Account();
        account.setCustomer(customer);
        account.setIban(iban);
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}
