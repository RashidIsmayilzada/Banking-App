package com.inholland.banking_app.models.factory;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionFactoryTest {

    private User initiator;
    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() {
        initiator = new User();
        initiator.setId(1L);
        initiator.setUsername("testuser");
        initiator.setRole(Role.CUSTOMER);
        initiator.setActive(true);
        initiator.setCreatedAt(LocalDateTime.now());
        initiator.setUpdatedAt(LocalDateTime.now());

        accountA = makeAccount("NL91INHO0417164300");
        accountB = makeAccount("NL91INHO0417164301");
    }

    // --- createTransfer ---

    @Test
    @DisplayName("createTransfer() - should set transaction type to TRANSFER")
    void createTransfer_shouldSetTypeToTransfer() {
        Transaction tx = TransactionFactory.createTransfer(accountA, accountB, new BigDecimal("100.00"), initiator, Channel.WEB, "desc");
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    @DisplayName("createTransfer() - should link from and to accounts correctly")
    void createTransfer_shouldLinkFromAndToAccounts() {
        Transaction tx = TransactionFactory.createTransfer(accountA, accountB, new BigDecimal("100.00"), initiator, Channel.WEB, "desc");
        assertThat(tx.getFromAccount()).isSameAs(accountA);
        assertThat(tx.getToAccount()).isSameAs(accountB);
    }

    @Test
    @DisplayName("createTransfer() - should set amount, currency, channel, initiatedBy and description")
    void createTransfer_shouldSetAllCoreFields() {
        BigDecimal amount = new BigDecimal("250.00");
        Transaction tx = TransactionFactory.createTransfer(accountA, accountB, amount, initiator, Channel.EMPLOYEE, "pay");

        assertThat(tx.getAmount()).isEqualByComparingTo(amount);
        assertThat(tx.getCurrency()).isEqualTo("EUR");
        assertThat(tx.getChannel()).isEqualTo(Channel.EMPLOYEE);
        assertThat(tx.getInitiatedBy()).isSameAs(initiator);
        assertThat(tx.getDescription()).isEqualTo("pay");
    }

    @Test
    @DisplayName("createTransfer() - should set createdAt to a non-null timestamp")
    void createTransfer_shouldSetCreatedAt() {
        Transaction tx = TransactionFactory.createTransfer(accountA, accountB, new BigDecimal("50.00"), initiator, Channel.WEB, "desc");
        assertThat(tx.getCreatedAt()).isNotNull();
    }

    // --- createDeposit ---

    @Test
    @DisplayName("createDeposit() - should set transaction type to DEPOSIT")
    void createDeposit_shouldSetTypeToDeposit() {
        Transaction tx = TransactionFactory.createDeposit(accountA, new BigDecimal("200.00"), initiator, Channel.ATM, "cash in");
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("createDeposit() - should set toAccount and leave fromAccount null")
    void createDeposit_shouldSetToAccountAndLeaveFromAccountNull() {
        Transaction tx = TransactionFactory.createDeposit(accountA, new BigDecimal("200.00"), initiator, Channel.ATM, "cash in");
        assertThat(tx.getToAccount()).isSameAs(accountA);
        assertThat(tx.getFromAccount()).isNull();
    }

    @Test
    @DisplayName("createDeposit() - should always use ATM channel")
    void createDeposit_shouldUseAtmChannel() {
        Transaction tx = TransactionFactory.createDeposit(accountA, new BigDecimal("200.00"), initiator, Channel.ATM, "cash in");
        assertThat(tx.getChannel()).isEqualTo(Channel.ATM);
    }

    @Test
    @DisplayName("createDeposit() - should set amount, currency, initiatedBy, description and createdAt")
    void createDeposit_shouldSetAllCoreFields() {
        BigDecimal amount = new BigDecimal("150.00");
        Transaction tx = TransactionFactory.createDeposit(accountA, amount, initiator, Channel.ATM, "deposit");

        assertThat(tx.getAmount()).isEqualByComparingTo(amount);
        assertThat(tx.getCurrency()).isEqualTo("EUR");
        assertThat(tx.getInitiatedBy()).isSameAs(initiator);
        assertThat(tx.getDescription()).isEqualTo("deposit");
        assertThat(tx.getCreatedAt()).isNotNull();
    }

    // --- createWithdrawal ---

    @Test
    @DisplayName("createWithdrawal() - should set transaction type to WITHDRAWAL")
    void createWithdrawal_shouldSetTypeToWithdrawal() {
        Transaction tx = TransactionFactory.createWithdrawal(accountA, new BigDecimal("50.00"), initiator, Channel.ATM, "cash out");
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    @DisplayName("createWithdrawal() - should set fromAccount and leave toAccount null")
    void createWithdrawal_shouldSetFromAccountAndLeaveToAccountNull() {
        Transaction tx = TransactionFactory.createWithdrawal(accountA, new BigDecimal("50.00"), initiator, Channel.ATM, "cash out");
        assertThat(tx.getFromAccount()).isSameAs(accountA);
        assertThat(tx.getToAccount()).isNull();
    }

    @Test
    @DisplayName("createWithdrawal() - should always use ATM channel")
    void createWithdrawal_shouldUseAtmChannel() {
        Transaction tx = TransactionFactory.createWithdrawal(accountA, new BigDecimal("50.00"), initiator, Channel.ATM, "cash out");
        assertThat(tx.getChannel()).isEqualTo(Channel.ATM);
    }

    @Test
    @DisplayName("createWithdrawal() - should set amount, currency, initiatedBy, description and createdAt")
    void createWithdrawal_shouldSetAllCoreFields() {
        BigDecimal amount = new BigDecimal("75.00");
        Transaction tx = TransactionFactory.createWithdrawal(accountA, amount, initiator, Channel.ATM, "withdrawal");

        assertThat(tx.getAmount()).isEqualByComparingTo(amount);
        assertThat(tx.getCurrency()).isEqualTo("EUR");
        assertThat(tx.getInitiatedBy()).isSameAs(initiator);
        assertThat(tx.getDescription()).isEqualTo("withdrawal");
        assertThat(tx.getCreatedAt()).isNotNull();
    }

    // --- helper ---

    private Account makeAccount(String iban) {
        Account account = new Account();
        account.setIban(iban);
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("500.00"));
        account.setAbsoluteTransferLimit(BigDecimal.ZERO);
        account.setDailyTransferLimit(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}
