package com.inholland.banking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.dtos.MoneyResponse;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.security.JwtAuthenticationFilter;
import com.inholland.banking_app.services.AccountService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice with security switched off, so it verifies request/response
 * wiring and exception-to-status mapping. Security and the role branching are
 * covered against the real filter chain in AccountControllerFunctionalTest.
 */
@WebMvcTest(
        value = AccountController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class AccountControllerTest {

    private static final String IBAN = "NL91ABNA0417164300";

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
                .ownerId(1L)
                .ownerUsername("customer")
                .iban(IBAN)
                .accountType(AccountType.CHECKING)
                .balance(MoneyResponse.eur(new BigDecimal("1000.00")))
                .absoluteTransferLimit(MoneyResponse.eur(new BigDecimal("5000.00")))
                .dailyTransferLimit(MoneyResponse.eur(new BigDecimal("2000.00")))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();

        Page<AccountResponse> page = new PageImpl<>(List.of(accountResponse), PageRequest.of(0, 10), 1);
        listResponse = AccountListResponse.of(page);

        customerAuth = authWith("customer", "ROLE_CUSTOMER");
        employeeAuth = authWith("employee", "ROLE_EMPLOYEE");
    }

    @Test
    void listAccounts_returnsOwnAccounts_whenCallerIsCustomer() throws Exception {
        when(accountService.listAccountsOwnedBy(anyString(), any())).thenReturn(listResponse);

        mockMvc.perform(get("/accounts").principal(customerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].iban").value(IBAN))
                .andExpect(jsonPath("$.totals.combinedBalance.amount").value(1000.00))
                .andExpect(jsonPath("$.totals.combinedBalance.currency").value("EUR"));

        // The role decides which service call is made: a customer must never list all accounts.
        verify(accountService).listAccountsOwnedBy(eq("customer"), any());
        verify(accountService, never()).listAccounts(any(), any());
    }

    @Test
    void listAccounts_filtersByUserId_whenCallerIsEmployee() throws Exception {
        when(accountService.listAccounts(eq(1L), any())).thenReturn(listResponse);

        mockMvc.perform(get("/accounts").param("userId", "1").principal(employeeAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isArray());

        verify(accountService).listAccounts(eq(1L), any());
        verify(accountService, never()).listAccountsOwnedBy(anyString(), any());
    }

    @Test
    void listAccounts_returnsEmptyEnvelope_whenCustomerHasNoAccounts() throws Exception {
        Page<AccountResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(accountService.listAccountsOwnedBy(anyString(), any())).thenReturn(AccountListResponse.of(emptyPage));

        mockMvc.perform(get("/accounts").principal(customerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isEmpty())
                .andExpect(jsonPath("$.totals.combinedBalance.amount").value(0));
    }

    @Test
    void getAccount_returnsAccountDetails_whenFound() throws Exception {
        when(accountService.getAccount(IBAN)).thenReturn(accountResponse);

        mockMvc.perform(get("/accounts/" + IBAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUsername").value("customer"))
                .andExpect(jsonPath("$.iban").value(IBAN))
                .andExpect(jsonPath("$.balance.amount").value(1000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAccount_returns404_whenAccountNotFound() throws Exception {
        when(accountService.getAccount(IBAN)).thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(get("/accounts/" + IBAN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void updateAccount_returns200_whenRequestIsValid() throws Exception {
        when(accountService.updateAccount(eq(IBAN), any(AccountUpdateRequest.class))).thenReturn(accountResponse);

        mockMvc.perform(patch("/accounts/" + IBAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(limitRequest("8000.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(IBAN));
    }

    @Test
    void updateAccount_returns404_whenAccountNotFound() throws Exception {
        when(accountService.updateAccount(eq(IBAN), any(AccountUpdateRequest.class)))
                .thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(patch("/accounts/" + IBAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(limitRequest("8000.00"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void updateAccount_returns422_whenLimitIsNegative() throws Exception {
        mockMvc.perform(patch("/accounts/" + IBAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(limitRequest("-1.00"))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateAccount_returns409_whenAccountIsClosed() throws Exception {
        when(accountService.updateAccount(eq(IBAN), any(AccountUpdateRequest.class)))
                .thenThrow(new AccountStateException("Cannot update a closed account"));

        mockMvc.perform(patch("/accounts/" + IBAN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(limitRequest("8000.00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_STATE_CONFLICT"));
    }

    private Authentication authWith(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(role)));
    }

    private AccountUpdateRequest limitRequest(String absoluteLimit) {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAbsoluteTransferLimit(new BigDecimal(absoluteLimit));
        return request;
    }
}
