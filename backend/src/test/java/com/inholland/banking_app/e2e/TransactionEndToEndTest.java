package com.inholland.banking_app.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DailyTransferUsageRepository dailyTransferUsageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "Test1234!@#$";

    // Customer A — the primary actor in most tests
    private Long customerAAccountId;
    private String customerAIban;
    private String customerAToken;

    // Customer B — transfer recipient
    private Long customerBAccountId;
    private String customerBIban;
    private String customerBToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanup();

        User customerA = createUser("txn-customer-a@test.com", "txncustomera", Role.CUSTOMER);
        createCustomerProfile(customerA, "Txn", "CustomerA", "400500600", "+31640050060");
        Account accountA = createCheckingAccount(customerA, "NL91INHO0200000001",
                new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("10000.00"));
        customerAAccountId = accountA.getId();
        customerAIban = accountA.getIban();
        customerAToken = login("txn-customer-a@test.com");

        User customerB = createUser("txn-customer-b@test.com", "txncustomerb", Role.CUSTOMER);
        createCustomerProfile(customerB, "Txn", "CustomerB", "700800900", "+31670080090");
        Account accountB = createCheckingAccount(customerB, "NL91INHO0200000002",
                new BigDecimal("3000.00"), BigDecimal.ZERO, new BigDecimal("10000.00"));
        customerBAccountId = accountB.getId();
        customerBIban = accountB.getIban();
        customerBToken = login("txn-customer-b@test.com");
    }

    @AfterEach
    void cleanup() {
        dailyTransferUsageRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerProfileRepository.deleteAll();
        employeeProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- Deposit ---

    @Test
    @DisplayName("DEPOSIT increases account balance and returns transaction result")
    void deposit_increasesBalance() throws Exception {
        String body = """
                {
                  "type": "DEPOSIT",
                  "accountId": %d,
                  "amount": 200.0,
                  "description": "Test deposit"
                }
                """.formatted(customerAAccountId);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.transaction.amount.amount").value(200.0));

        BigDecimal newBalance = accountRepository.findById(customerAAccountId)
                .orElseThrow().getBalance();
        assertThat(newBalance).isEqualByComparingTo("5200.00");
    }

    // --- Withdrawal ---

    @Test
    @DisplayName("WITHDRAWAL decreases account balance and returns transaction result")
    void withdrawal_decreasesBalance() throws Exception {
        String body = """
                {
                  "type": "WITHDRAWAL",
                  "accountId": %d,
                  "amount": 300.0,
                  "description": "Test withdrawal"
                }
                """.formatted(customerAAccountId);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.transaction.amount.amount").value(300.0));

        BigDecimal newBalance = accountRepository.findById(customerAAccountId)
                .orElseThrow().getBalance();
        assertThat(newBalance).isEqualByComparingTo("4700.00");
    }

    @Test
    @DisplayName("WITHDRAWAL with insufficient funds returns 400")
    void withdrawal_insufficientFunds_returns400() throws Exception {
        String body = """
                {
                  "type": "WITHDRAWAL",
                  "accountId": %d,
                  "amount": 99999.0,
                  "description": "Too much"
                }
                """.formatted(customerAAccountId);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("WITHDRAWAL from another customer's account returns 403")
    void withdrawal_fromOtherCustomerAccount_returns403() throws Exception {
        String body = """
                {
                  "type": "WITHDRAWAL",
                  "accountId": %d,
                  "amount": 50.0,
                  "description": "Unauthorized withdrawal"
                }
                """.formatted(customerBAccountId);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // --- Transfer ---

    @Test
    @DisplayName("TRANSFER via IBAN updates both account balances")
    void transfer_viaIban_updatesBothBalances() throws Exception {
        String body = """
                {
                  "type": "TRANSFER",
                  "fromAccountId": %d,
                  "toIban": "%s",
                  "amount": 500.0,
                  "description": "Test transfer"
                }
                """.formatted(customerAAccountId, customerBIban);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.transaction.amount.amount").value(500.0))
                .andExpect(jsonPath("$.sourceBalance.amount").value(4500.0))
                .andExpect(jsonPath("$.destinationBalance.amount").value(3500.0));

        assertThat(accountRepository.findById(customerAAccountId).orElseThrow().getBalance())
                .isEqualByComparingTo("4500.00");
        assertThat(accountRepository.findById(customerBAccountId).orElseThrow().getBalance())
                .isEqualByComparingTo("3500.00");
    }

    @Test
    @DisplayName("TRANSFER from another customer's account returns 403")
    void transfer_fromOtherCustomerAccount_returns403() throws Exception {
        String body = """
                {
                  "type": "TRANSFER",
                  "fromAccountId": %d,
                  "toIban": "%s",
                  "amount": 100.0,
                  "description": "Unauthorized transfer"
                }
                """.formatted(customerBAccountId, customerAIban);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TRANSFER with insufficient funds returns 400")
    void transfer_insufficientFunds_returns400() throws Exception {
        String body = """
                {
                  "type": "TRANSFER",
                  "fromAccountId": %d,
                  "toIban": "%s",
                  "amount": 99999.0,
                  "description": "Too much"
                }
                """.formatted(customerAAccountId, customerBIban);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- List transactions ---

    @Test
    @DisplayName("GET /transactions returns only the authenticated customer's transactions")
    void getTransactions_customerSeesOnlyOwnTransactions() throws Exception {
        // Customer A makes a deposit
        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DEPOSIT\",\"accountId\":%d,\"amount\":100.0,\"description\":\"A deposit\"}"
                                .formatted(customerAAccountId)))
                .andExpect(status().isCreated());

        // Customer B makes a deposit
        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + customerBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DEPOSIT\",\"accountId\":%d,\"amount\":50.0,\"description\":\"B deposit\"}"
                                .formatted(customerBAccountId)))
                .andExpect(status().isCreated());

        // Customer A should only see their own transaction
        String response = mockMvc.perform(get("/transactions?page=0&size=10")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andReturn().getResponse().getContentAsString();

        String description = objectMapper.readTree(response)
                .path("items").get(0).path("description").asText();
        assertThat(description).isEqualTo("A deposit");
    }

    @Test
    @DisplayName("GET /transactions with no transactions returns empty page")
    void getTransactions_noTransactions_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/transactions?page=0&size=10")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    // --- Helpers ---

    private User createUser(String email, String username, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private void createCustomerProfile(User user, String firstName, String lastName,
                                        String bsn, String phone) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setBsn(bsn);
        profile.setPhoneNumber(phone);
        profile.setStatus(CustomerStatus.APPROVED);
        profile.setRegisteredAt(LocalDateTime.now());
        customerProfileRepository.save(profile);
    }

    private Account createCheckingAccount(User owner, String iban, BigDecimal balance,
                                           BigDecimal absLimit, BigDecimal dailyLimit) {
        Account account = new Account();
        account.setCustomer(owner);
        account.setIban(iban);
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(balance);
        account.setAbsoluteTransferLimit(absLimit);
        account.setDailyTransferLimit(dailyLimit);
        account.setActive(true);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
