// --Efe(Admin)
package com.inholland.banking_app.controllers;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for admin-only account lifecycle endpoints:
 * PATCH /accounts/{iban}/freeze, /unfreeze, /close.
 * Full application context, real security filter chain.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountAdminFunctionalTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // --Efe(Admin) — AccountService.getCurrentAdmin() does a DB lookup by the authenticated username.
        // @WithMockUser sets the SecurityContext principal but does not seed the database,
        // so we create the admin user here so the lookup succeeds.
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setEmail("admin@bank.com");
            admin.setUsername("admin");
            admin.setPasswordHash("hashed");
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            userRepository.save(admin);
        }
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void freezeAccount_asAdmin_returnsOkAndStatusFrozen() throws Exception {
        Account account = createActiveAccount("NL01INHO0000000099");

        mockMvc.perform(patch("/accounts/" + account.getIban() + "/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void freezeAccount_asEmployee_isForbidden() throws Exception {
        Account account = createActiveAccount("NL01INHO0000000098");

        mockMvc.perform(patch("/accounts/" + account.getIban() + "/freeze"))
                .andExpect(status().isForbidden());
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void freezeAccount_asCustomer_isForbidden() throws Exception {
        Account account = createActiveAccount("NL01INHO0000000097");

        mockMvc.perform(patch("/accounts/" + account.getIban() + "/freeze"))
                .andExpect(status().isForbidden());
    }

    // --Efe(Admin)
    @Test
    void freezeAccount_anonymous_isUnauthorized() throws Exception {
        Account account = createActiveAccount("NL01INHO0000000096");

        mockMvc.perform(patch("/accounts/" + account.getIban() + "/freeze"))
                .andExpect(status().isUnauthorized());
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void freezeAccount_asAdmin_returns409_whenAlreadyFrozen() throws Exception {
        Account account = createActiveAccount("NL01INHO0000000095");
        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);

        mockMvc.perform(patch("/accounts/" + account.getIban() + "/freeze"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_STATE_CONFLICT"));
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unfreezeAccount_asAdmin_returnsOkAndStatusActive() throws Exception {
        Account account = createActiveAccount("NL01INHO0000000094");
        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);

        mockMvc.perform(patch("/accounts/" + account.getIban() + "/unfreeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unfreezeAccount_asAdmin_returns409_whenNotFrozen() throws Exception {
        Account account = createActiveAccount("NL01INHO0000000093");

        mockMvc.perform(patch("/accounts/" + account.getIban() + "/unfreeze"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_STATE_CONFLICT"));
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void closeAccount_asAdmin_returnsOkAndStatusClosed() throws Exception {
        Account account = createActiveAccount("NL01INHO0000000092");

        mockMvc.perform(patch("/accounts/" + account.getIban() + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void closeAccount_asAdmin_returns409_whenAlreadyClosed() throws Exception {
        Account account = createActiveAccount("NL01INHO0000000091");
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);

        mockMvc.perform(patch("/accounts/" + account.getIban() + "/close"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_STATE_CONFLICT"));
    }

    // --- Fixtures ---

    private Account createActiveAccount(String iban) {
        User owner = new User();
        owner.setEmail(iban + "@bank.com");
        owner.setUsername("owner_" + iban);
        owner.setPasswordHash("hashed");
        owner.setRole(Role.CUSTOMER);
        owner.setActive(true);
        owner.setCreatedAt(LocalDateTime.now());
        owner.setUpdatedAt(LocalDateTime.now());
        userRepository.save(owner);

        Account account = new Account();
        account.setCustomer(owner);
        account.setIban(iban);
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(BigDecimal.ZERO);
        account.setDailyTransferLimit(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }
}
