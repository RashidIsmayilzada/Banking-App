package com.inholland.banking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for the account endpoints: full application context, security
 * on (real filter chain), real service and repositories. Data is seeded through
 * the repositories and rolled back after each test via @Transactional.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountControllerFunctionalTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // springSecurity() wires the real filter chain into MockMvc so @WithMockUser
        // is honoured; @AutoConfigureMockMvc alone does not propagate it through this
        // app's stateless JWT chain.
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // --- GET /accounts (employee) ---

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void listAccounts_asEmployee_returnsEveryAccount() throws Exception {
        User alice = createCustomer("alice");
        createAccount(alice, "NL01INHO0000000001", AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());
        createAccount(alice, "NL01INHO0000000002", AccountType.SAVINGS, AccountStatus.ACTIVE, "500.00", LocalDateTime.now());
        createAccount(createCustomer("bob"), "NL02INHO0000000001", AccountType.CHECKING, AccountStatus.ACTIVE, "300.00", LocalDateTime.now());

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts", hasSize(3)))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void listAccounts_asEmployee_filtersByUserId() throws Exception {
        User alice = createCustomer("alice");
        createAccount(alice, "NL01INHO0000000001", AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());
        createAccount(alice, "NL01INHO0000000002", AccountType.SAVINGS, AccountStatus.ACTIVE, "500.00", LocalDateTime.now());
        createAccount(createCustomer("bob"), "NL02INHO0000000001", AccountType.CHECKING, AccountStatus.ACTIVE, "300.00", LocalDateTime.now());

        mockMvc.perform(get("/accounts").param("userId", alice.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts", hasSize(2)))
                .andExpect(jsonPath("$.accounts[*].ownerUsername", everyItem(is("alice"))));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void listAccounts_asEmployee_paginatesWithTenPerPage() throws Exception {
        User owner = createCustomer("owner");
        for (int i = 1; i <= 15; i++) {
            createAccount(owner, String.format("NL00INHO%010d", i),
                    AccountType.CHECKING, AccountStatus.ACTIVE, "100.00", LocalDateTime.now());
        }

        mockMvc.perform(get("/accounts").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts", hasSize(10)))
                .andExpect(jsonPath("$.page.currentPage").value(0))
                .andExpect(jsonPath("$.page.pageSize").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(15))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/accounts").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts", hasSize(5)))
                .andExpect(jsonPath("$.page.currentPage").value(1));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void listAccounts_returnsEnvelopeWithCombinedBalance() throws Exception {
        User owner = createCustomer("owner");
        createAccount(owner, "NL01INHO0000000001", AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());
        createAccount(owner, "NL01INHO0000000002", AccountType.SAVINGS, AccountStatus.ACTIVE, "500.00", LocalDateTime.now());

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.totals.combinedBalance.amount").value(1500.00))
                .andExpect(jsonPath("$.totals.combinedBalance.currency").value("EUR"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void listAccounts_asEmployee_ordersByCreatedAtDescending() throws Exception {
        User owner = createCustomer("owner");
        createAccount(owner, "NL00INHO0000000001", AccountType.CHECKING, AccountStatus.ACTIVE, "100.00", LocalDateTime.now().minusDays(2));
        createAccount(owner, "NL00INHO0000000002", AccountType.CHECKING, AccountStatus.ACTIVE, "100.00", LocalDateTime.now().minusDays(1));
        createAccount(owner, "NL00INHO0000000003", AccountType.CHECKING, AccountStatus.ACTIVE, "100.00", LocalDateTime.now());

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].iban").value("NL00INHO0000000003"))
                .andExpect(jsonPath("$.accounts[2].iban").value("NL00INHO0000000001"));
    }

    // --- GET /accounts (customer) ---

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void listAccounts_asCustomer_returnsOnlyOwnAccounts() throws Exception {
        User alice = createCustomer("alice");
        createAccount(alice, "NL01INHO0000000001", AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());
        createAccount(alice, "NL01INHO0000000002", AccountType.SAVINGS, AccountStatus.ACTIVE, "500.00", LocalDateTime.now());
        createAccount(createCustomer("bob"), "NL02INHO0000000001", AccountType.CHECKING, AccountStatus.ACTIVE, "300.00", LocalDateTime.now());

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts", hasSize(2)))
                .andExpect(jsonPath("$.accounts[*].ownerUsername", everyItem(is("alice"))));
    }

    @Test
    @WithMockUser(username = "loner", roles = "CUSTOMER")
    void listAccounts_asCustomer_returnsEmpty_whenOwningNothing() throws Exception {
        createAccount(createCustomer("someoneElse"), "NL02INHO0000000001",
                AccountType.CHECKING, AccountStatus.ACTIVE, "300.00", LocalDateTime.now());

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts", hasSize(0)))
                .andExpect(jsonPath("$.totals.combinedBalance.amount").value(0));
    }

    // --- GET /accounts/{iban} ---

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAccount_asEmployee_readsAnyAccount() throws Exception {
        createAccount(createCustomer("alice"), "NL01INHO0000000001",
                AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());

        mockMvc.perform(get("/accounts/NL01INHO0000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value("NL01INHO0000000001"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void getAccount_asOwner_readsOwnAccount() throws Exception {
        createAccount(createCustomer("alice"), "NL01INHO0000000001",
                AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());

        mockMvc.perform(get("/accounts/NL01INHO0000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUsername").value("alice"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void getAccount_asOtherCustomer_isForbidden() throws Exception {
        createAccount(createCustomer("bob"), "NL02INHO0000000001",
                AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());

        mockMvc.perform(get("/accounts/NL02INHO0000000001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAccount_returns404_whenIbanUnknown() throws Exception {
        mockMvc.perform(get("/accounts/NL00BANK0000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getAccount_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(get("/accounts/NL01INHO0000000001"))
                .andExpect(status().isUnauthorized());
    }

    // --- PATCH /accounts/{iban} ---

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateAccount_asEmployee_updatesBothLimits() throws Exception {
        createAccount(createCustomer("alice"), "NL01INHO0000000001",
                AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());

        mockMvc.perform(patch("/accounts/NL01INHO0000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("8000.00", "3000.00", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.absoluteTransferLimit.amount").value(8000.00))
                .andExpect(jsonPath("$.dailyTransferLimit.amount").value(3000.00));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateAccount_asEmployee_updatesOnlyProvidedLimit() throws Exception {
        Account account = createAccount(createCustomer("alice"), "NL01INHO0000000001",
                AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
        accountRepository.save(account);

        mockMvc.perform(patch("/accounts/NL01INHO0000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("8000.00", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.absoluteTransferLimit.amount").value(8000.00))
                .andExpect(jsonPath("$.dailyTransferLimit.amount").value(2000.00));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateAccount_asEmployee_closesAccount() throws Exception {
        createAccount(createCustomer("alice"), "NL01INHO0000000001",
                AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());

        mockMvc.perform(patch("/accounts/NL01INHO0000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(null, null, AccountStatus.CLOSED)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateAccount_returns409_whenAccountIsClosed() throws Exception {
        createAccount(createCustomer("alice"), "NL01INHO0000000001",
                AccountType.CHECKING, AccountStatus.CLOSED, "1000.00", LocalDateTime.now());

        mockMvc.perform(patch("/accounts/NL01INHO0000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("8000.00", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_STATE_CONFLICT"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateAccount_returns404_whenIbanUnknown() throws Exception {
        mockMvc.perform(patch("/accounts/NL00BANK0000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("8000.00", null, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateAccount_returns422_whenLimitIsNegative() throws Exception {
        createAccount(createCustomer("alice"), "NL01INHO0000000001",
                AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());

        mockMvc.perform(patch("/accounts/NL01INHO0000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("-1.00", null, null)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void updateAccount_asCustomer_isForbidden() throws Exception {
        createAccount(createCustomer("alice"), "NL01INHO0000000001",
                AccountType.CHECKING, AccountStatus.ACTIVE, "1000.00", LocalDateTime.now());

        mockMvc.perform(patch("/accounts/NL01INHO0000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("8000.00", null, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAccount_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(patch("/accounts/NL01INHO0000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("8000.00", null, null)))
                .andExpect(status().isUnauthorized());
    }

    // --- Fixtures ---

    private User createCustomer(String username) {
        User user = new User();
        user.setEmail(username + "@bank.com");
        user.setUsername(username);
        user.setPasswordHash("hashed");
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private Account createAccount(User owner, String iban, AccountType type, AccountStatus accountStatus,
                                  String balance, LocalDateTime createdAt) {
        Account account = new Account();
        account.setCustomer(owner);
        account.setIban(iban);
        account.setAccountType(type);
        account.setBalance(new BigDecimal(balance));
        account.setAbsoluteTransferLimit(BigDecimal.ZERO);
        account.setDailyTransferLimit(new BigDecimal("1000.00"));
        account.setStatus(accountStatus);
        account.setCreatedAt(createdAt);
        return accountRepository.save(account);
    }

    private String updateBody(String absoluteLimit, String dailyLimit, AccountStatus accountStatus) throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest();
        if (absoluteLimit != null) {
            request.setAbsoluteTransferLimit(new BigDecimal(absoluteLimit));
        }
        if (dailyLimit != null) {
            request.setDailyTransferLimit(new BigDecimal(dailyLimit));
        }
        request.setStatus(accountStatus);
        return objectMapper.writeValueAsString(request);
    }
}
