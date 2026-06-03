package com.inholland.banking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inholland.banking_app.dtos.*;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.security.JwtAuthenticationFilter;
import com.inholland.banking_app.services.AccountService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AccountController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AccountResponse accountResponse;
    private AccountListResponse listResponse;
    private Authentication customerAuth;
    private Authentication employeeAuth;

    @BeforeEach
    void setUp() {
        accountResponse = AccountResponse.builder()
                .accountId(10L)
                .ownerId(1L)
                .ownerUsername("customer")
                .iban("NL91ABNA0417164300")
                .accountType(AccountType.CHECKING)
                .balance(MoneyResponse.eur(new BigDecimal("1000.00")))
                .absoluteTransferLimit(MoneyResponse.eur(new BigDecimal("5000.00")))
                .dailyTransferLimit(MoneyResponse.eur(new BigDecimal("2000.00")))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();

        List<AccountResponse> accounts = List.of(accountResponse);
        Page<AccountResponse> page = new PageImpl<>(accounts, PageRequest.of(0, 10), 1);
        listResponse = AccountListResponse.of(page);

        customerAuth = new UsernamePasswordAuthenticationToken("customer", null, Collections.emptyList());
        employeeAuth = new UsernamePasswordAuthenticationToken("employee", null, Collections.emptyList());
    }

    // --- GET /accounts ---

    @Test
    @DisplayName("GET /accounts - should return 200 with account list")
    void listAccounts_shouldReturn200_withAccountList() throws Exception {
        when(accountService.listAccounts(isNull(), anyString(), any())).thenReturn(listResponse);

        mockMvc.perform(get("/accounts").principal(customerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts[0].accountId").value(10))
                .andExpect(jsonPath("$.accounts[0].iban").value("NL91ABNA0417164300"))
                .andExpect(jsonPath("$.totals.combinedBalance.amount").value(1000.00))
                .andExpect(jsonPath("$.totals.combinedBalance.currency").value("EUR"));
    }

    @Test
    @DisplayName("GET /accounts?userId=1 - should return 200 with filtered accounts")
    void listAccounts_shouldReturn200_withUserIdFilter() throws Exception {
        when(accountService.listAccounts(eq(1L), anyString(), any())).thenReturn(listResponse);

        mockMvc.perform(get("/accounts").param("userId", "1").principal(employeeAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isArray());
    }

    @Test
    @DisplayName("GET /accounts - should return 200 with empty list when no accounts exist")
    void listAccounts_shouldReturn200_withEmptyList() throws Exception {
        Page<AccountResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        AccountListResponse emptyResponse = AccountListResponse.of(emptyPage);

        when(accountService.listAccounts(isNull(), anyString(), any())).thenReturn(emptyResponse);

        mockMvc.perform(get("/accounts").principal(customerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isEmpty())
                .andExpect(jsonPath("$.totals.combinedBalance.amount").value(0));
    }

    // --- GET /accounts/{accountId} ---

    @Test
    @DisplayName("GET /accounts/{accountId} - should return 200 with account details")
    void getAccount_shouldReturn200_whenAccountExists() throws Exception {
        when(accountService.getAccount(10L)).thenReturn(accountResponse);

        mockMvc.perform(get("/accounts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.ownerId").value(1))
                .andExpect(jsonPath("$.ownerUsername").value("customer"))
                .andExpect(jsonPath("$.iban").value("NL91ABNA0417164300"))
                .andExpect(jsonPath("$.balance.amount").value(1000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /accounts/{accountId} - should return 404 when account not found")
    void getAccount_shouldReturn404_whenAccountNotFound() throws Exception {
        when(accountService.getAccount(99L)).thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(get("/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    // --- PATCH /accounts/{accountId} ---

    @Test
    @DisplayName("PATCH /accounts/{accountId} - should return 200 when update is valid")
    void updateAccount_shouldReturn200_whenRequestIsValid() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal("8000.00"));
        request.setDailyTransferLimit(new BigDecimal("3000.00"));

        when(accountService.updateAccount(eq(10L), any(AccountUpdateRequest.class))).thenReturn(accountResponse);

        mockMvc.perform(patch("/accounts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(10));
    }

    @Test
    @DisplayName("PATCH /accounts/{accountId} - should return 404 when account not found")
    void updateAccount_shouldReturn404_whenAccountNotFound() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal("8000.00"));

        when(accountService.updateAccount(eq(99L), any(AccountUpdateRequest.class)))
                .thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(patch("/accounts/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /accounts/{accountId} - should return 422 when limit is negative")
    void updateAccount_shouldReturn422_whenLimitIsNegative() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal("-1.00"));

        mockMvc.perform(patch("/accounts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH /accounts/{accountId} - should return 409 when account is in a conflicting state (closed)")
    void updateAccount_shouldReturn409_whenAccountStateConflict() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal("8000.00"));

        when(accountService.updateAccount(eq(10L), any(AccountUpdateRequest.class)))
                .thenThrow(new AccountStateException("Cannot update a closed account"));

        mockMvc.perform(patch("/accounts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_STATE_CONFLICT"));
    }
}
