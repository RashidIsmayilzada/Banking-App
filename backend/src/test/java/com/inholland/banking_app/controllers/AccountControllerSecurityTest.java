package com.inholland.banking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.dtos.MoneyResponse;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.services.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full app (security on) to check the @PreAuthorize/@PostAuthorize
 * rules on the account endpoints. {@link AccountControllerTest} runs with
 * security off, so it can't catch these. The service is mocked so each test
 * picks the account's owner and just asserts 403 vs 200.
 */
@SpringBootTest
class AccountControllerSecurityTest {

    private static final String IBAN = "NL91ABNA0417164300";

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AccountService accountService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // springSecurity() runs requests through the real filter chain so
        // @WithMockUser actually logs in.
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private AccountResponse accountOwnedBy(String ownerUsername) {
        return AccountResponse.builder()
                .ownerId(1L)
                .ownerUsername(ownerUsername)
                .iban(IBAN)
                .accountType(AccountType.CHECKING)
                .balance(MoneyResponse.eur(new BigDecimal("1000.00")))
                .absoluteTransferLimit(MoneyResponse.eur(new BigDecimal("5000.00")))
                .dailyTransferLimit(MoneyResponse.eur(new BigDecimal("2000.00")))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();
    }

    private String updateBody() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal("8000.00"));
        request.setDailyTransferLimit(new BigDecimal("3000.00"));
        return objectMapper.writeValueAsString(request);
    }

    // --- PATCH /accounts/{iban} : @PreAuthorize("hasRole('EMPLOYEE')") ---

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    @DisplayName("PATCH /accounts/{iban} - CUSTOMER is forbidden (403)")
    void updateAccount_asCustomer_isForbidden() throws Exception {
        mockMvc.perform(patch("/accounts/" + IBAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    @DisplayName("PATCH /accounts/{iban} - EMPLOYEE succeeds (200)")
    void updateAccount_asEmployee_succeeds() throws Exception {
        when(accountService.updateAccount(eq(IBAN), any(AccountUpdateRequest.class)))
                .thenReturn(accountOwnedBy("someCustomer"));

        mockMvc.perform(patch("/accounts/" + IBAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isOk());
    }

    // --- GET /accounts/{iban} : @PostAuthorize owner-or-employee ---

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    @DisplayName("GET /accounts/{iban} - CUSTOMER reading another customer's account is forbidden (403)")
    void getAccount_customerReadingAnothersAccount_isForbidden() throws Exception {
        when(accountService.getAccount(IBAN)).thenReturn(accountOwnedBy("anotherCustomer"));

        mockMvc.perform(get("/accounts/" + IBAN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    @DisplayName("GET /accounts/{iban} - CUSTOMER reading their own account succeeds (200)")
    void getAccount_customerReadingOwnAccount_succeeds() throws Exception {
        when(accountService.getAccount(IBAN)).thenReturn(accountOwnedBy("customer"));

        mockMvc.perform(get("/accounts/" + IBAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUsername").value("customer"));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    @DisplayName("GET /accounts/{iban} - EMPLOYEE reading any account succeeds (200)")
    void getAccount_employeeReadingAnyAccount_succeeds() throws Exception {
        when(accountService.getAccount(IBAN)).thenReturn(accountOwnedBy("anotherCustomer"));

        mockMvc.perform(get("/accounts/" + IBAN))
                .andExpect(status().isOk());
    }

    // --- Anonymous (no token) : rejected by the filter chain before reaching the controller ---

    @Test
    @DisplayName("GET /accounts - anonymous request is unauthorized (401)")
    void listAccounts_asAnonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /accounts/{iban} - anonymous request is unauthorized (401)")
    void getAccount_asAnonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get("/accounts/" + IBAN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("PATCH /accounts/{iban} - anonymous request is unauthorized (401)")
    void updateAccount_asAnonymous_isUnauthorized() throws Exception {
        mockMvc.perform(patch("/accounts/" + IBAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
