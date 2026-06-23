// --Efe(Admin)
package com.inholland.banking_app.controllers;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for POST /transactions/{id}/reverse.
 * Full application context, real security filter chain.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionAdminFunctionalTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private MockMvc mockMvc;

    // --Efe(Admin) — reverseTransaction does a DB lookup by authenticated username,
    // so the "admin" user must exist in the test database.
    private User admin;
    private User customer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setEmail("admin_func@bank.com");
            u.setUsername("admin");
            u.setPasswordHash("hashed");
            u.setRole(Role.ADMIN);
            u.setActive(true);
            u.setCreatedAt(LocalDateTime.now());
            u.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(u);
        });

        customer = createUser("txcustomer", Role.CUSTOMER);
    }

    // --- Security ---

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void reverseTransaction_asAdmin_returnsOk() throws Exception {
        Transaction tx = seedTransfer(new BigDecimal("100.00"));

        mockMvc.perform(post("/transactions/" + tx.getId() + "/reverse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalTransactionId").value(tx.getId()))
                .andExpect(jsonPath("$.transactionType").value("REVERSAL"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void reverseTransaction_asEmployee_isForbidden() throws Exception {
        Transaction tx = seedTransfer(new BigDecimal("50.00"));

        mockMvc.perform(post("/transactions/" + tx.getId() + "/reverse"))
                .andExpect(status().isForbidden());
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void reverseTransaction_asCustomer_isForbidden() throws Exception {
        Transaction tx = seedTransfer(new BigDecimal("50.00"));

        mockMvc.perform(post("/transactions/" + tx.getId() + "/reverse"))
                .andExpect(status().isForbidden());
    }

    // --Efe(Admin)
    @Test
    void reverseTransaction_anonymous_isUnauthorized() throws Exception {
        Transaction tx = seedTransfer(new BigDecimal("50.00"));

        mockMvc.perform(post("/transactions/" + tx.getId() + "/reverse"))
                .andExpect(status().isUnauthorized());
    }

    // --- Transaction types ---

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void reverseDeposit_asAdmin_returnsOk() throws Exception {
        Account account = createAccount("NL99INHO0000000201", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        Transaction tx = seedDeposit(account, new BigDecimal("100.00"));

        mockMvc.perform(post("/transactions/" + tx.getId() + "/reverse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("REVERSAL"));
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void reverseWithdrawal_asAdmin_returnsOk() throws Exception {
        Account account = createAccount("NL99INHO0000000202", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        Transaction tx = seedWithdrawal(account, new BigDecimal("100.00"));

        mockMvc.perform(post("/transactions/" + tx.getId() + "/reverse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("REVERSAL"));
    }

    // --- Error cases ---

    // --Efe(Admin)
    // IllegalArgumentException ("Transaction not found") → 400 BAD_REQUEST (handled by GlobalExceptionHandler)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void reverseTransaction_returns400_whenTransactionNotFound() throws Exception {
        mockMvc.perform(post("/transactions/999999/reverse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // --Efe(Admin)
    // IllegalStateException ("already reversed") → 500 because GlobalExceptionHandler
    // has no explicit handler for IllegalStateException; falls through to Exception.class → 500.
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void reverseTransaction_returns500_whenAlreadyReversed() throws Exception {
        Transaction original = seedTransfer(new BigDecimal("100.00"));

        Transaction reversal = new Transaction();
        reversal.setTransactionType(TransactionType.REVERSAL);
        reversal.setFromAccount(original.getToAccount());
        reversal.setToAccount(original.getFromAccount());
        reversal.setAmount(original.getAmount());
        reversal.setCurrency("EUR");
        reversal.setChannel(Channel.EMPLOYEE);
        reversal.setInitiatedBy(admin);
        reversal.setCreatedAt(LocalDateTime.now());
        reversal.setDescription("Reversal of transaction #" + original.getId());
        reversal.setReversesTransaction(original);
        transactionRepository.save(reversal);

        mockMvc.perform(post("/transactions/" + original.getId() + "/reverse"))
                .andExpect(status().isInternalServerError());
    }

    // --Efe(Admin)
    // AccountStateException ("destination account is closed") → 409 ACCOUNT_STATE_CONFLICT
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void reverseTransaction_returns409_whenDestinationAccountClosed() throws Exception {
        Transaction tx = seedTransfer(new BigDecimal("100.00"));

        Account toAccount = tx.getToAccount();
        toAccount.setStatus(AccountStatus.CLOSED);
        toAccount.setClosedAt(LocalDateTime.now());
        accountRepository.save(toAccount);

        mockMvc.perform(post("/transactions/" + tx.getId() + "/reverse"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_STATE_CONFLICT"));
    }

    // --- Fixtures ---

    private Transaction seedTransfer(BigDecimal amount) {
        Account from = createAccount("NL99INHO0000000101", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        Account to   = createAccount("NL99INHO0000000102", new BigDecimal("1000.00"), AccountStatus.ACTIVE);

        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setAmount(amount);
        tx.setCurrency("EUR");
        tx.setChannel(Channel.WEB);
        tx.setInitiatedBy(customer);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setDescription("Test transfer");
        return transactionRepository.save(tx);
    }

    private Transaction seedDeposit(Account toAccount, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setToAccount(toAccount);
        tx.setAmount(amount);
        tx.setCurrency("EUR");
        tx.setChannel(Channel.ATM);
        tx.setInitiatedBy(customer);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setDescription("Test deposit");
        return transactionRepository.save(tx);
    }

    private Transaction seedWithdrawal(Account fromAccount, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.WITHDRAWAL);
        tx.setFromAccount(fromAccount);
        tx.setAmount(amount);
        tx.setCurrency("EUR");
        tx.setChannel(Channel.ATM);
        tx.setInitiatedBy(customer);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setDescription("Test withdrawal");
        return transactionRepository.save(tx);
    }

    private Account createAccount(String iban, BigDecimal balance, AccountStatus status) {
        Account account = new Account();
        account.setIban(iban);
        account.setCustomer(customer);
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(balance);
        account.setAbsoluteTransferLimit(BigDecimal.ZERO);
        account.setDailyTransferLimit(new BigDecimal("5000.00"));
        account.setStatus(status);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    private User createUser(String username, Role role) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setEmail(username + "@bank.com");
            u.setUsername(username);
            u.setPasswordHash("hashed");
            u.setRole(role);
            u.setActive(true);
            u.setCreatedAt(LocalDateTime.now());
            u.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(u);
        });
    }
}
