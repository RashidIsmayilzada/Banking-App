package com.inholland.banking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inholland.banking_app.dtos.MoneyDto;
import com.inholland.banking_app.dtos.PageMetadataDto;
import com.inholland.banking_app.dtos.TransactionDto;
import com.inholland.banking_app.dtos.TransactionPageDto;
import com.inholland.banking_app.dtos.TransactionRequest;
import com.inholland.banking_app.dtos.TransactionResultDto;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.security.JwtAuthenticationFilter;
import com.inholland.banking_app.services.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = TransactionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TransactionRequest validTransferRequest;

    @BeforeEach
    void setUp() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        validTransferRequest = new TransactionRequest();
        validTransferRequest.setType(TransactionType.TRANSFER);
        validTransferRequest.setFromAccountId(1L);
        validTransferRequest.setToAccountId(2L);
        validTransferRequest.setAmount(100.0);
        validTransferRequest.setDescription("Test transfer");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /transactions - should return 200 with transaction page")
    void listTransactions_shouldReturn200_withTransactionPage() throws Exception {
        TransactionPageDto pageDto = TransactionPageDto.builder()
                .items(List.of())
                .page(PageMetadataDto.builder()
                        .page(0).size(10).totalElements(0L).totalPages(0)
                        .build())
                .build();

        when(transactionService.listTransactions(any(), anyString())).thenReturn(pageDto);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(10));
    }

    @Test
    @DisplayName("POST /transactions - should return 201 when TRANSFER request is valid")
    void createTransaction_shouldReturn201_whenTransferRequestIsValid() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequest.class), anyString()))
                .thenReturn(buildTransactionResult(TransactionType.TRANSFER, 100.0));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.transaction.amount.amount").value(100.0))
                .andExpect(jsonPath("$.sourceBalance.currency").value("EUR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 201 when DEPOSIT request is valid")
    void createTransaction_shouldReturn201_whenDepositRequestIsValid() throws Exception {
        TransactionRequest depositRequest = new TransactionRequest();
        depositRequest.setType(TransactionType.DEPOSIT);
        depositRequest.setAccountId(1L);
        depositRequest.setAmount(200.0);
        depositRequest.setDescription("Cash deposit");

        when(transactionService.createTransaction(any(TransactionRequest.class), anyString()))
                .thenReturn(buildTransactionResult(TransactionType.DEPOSIT, 200.0));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.transaction.amount.amount").value(200.0));
    }

    @Test
    @DisplayName("POST /transactions - should return 201 when WITHDRAWAL request is valid")
    void createTransaction_shouldReturn201_whenWithdrawalRequestIsValid() throws Exception {
        TransactionRequest withdrawalRequest = new TransactionRequest();
        withdrawalRequest.setType(TransactionType.WITHDRAWAL);
        withdrawalRequest.setAccountId(1L);
        withdrawalRequest.setAmount(50.0);
        withdrawalRequest.setDescription("ATM withdrawal");

        when(transactionService.createTransaction(any(TransactionRequest.class), anyString()))
                .thenReturn(buildTransactionResult(TransactionType.WITHDRAWAL, 50.0));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.transaction.amount.amount").value(50.0));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when transaction type is missing")
    void createTransaction_shouldReturn422_whenTypeIsMissing() throws Exception {
        validTransferRequest.setType(null);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when amount is null")
    void createTransaction_shouldReturn422_whenAmountIsNull() throws Exception {
        validTransferRequest.setAmount(null);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when amount is negative")
    void createTransaction_shouldReturn422_whenAmountIsNegative() throws Exception {
        validTransferRequest.setAmount(-50.0);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when description is blank")
    void createTransaction_shouldReturn422_whenDescriptionIsBlank() throws Exception {
        validTransferRequest.setDescription("");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when description exceeds 255 characters")
    void createTransaction_shouldReturn422_whenDescriptionExceeds255Characters() throws Exception {
        validTransferRequest.setDescription("x".repeat(256));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when toIban has invalid format")
    void createTransaction_shouldReturn422_whenToIbanHasInvalidFormat() throws Exception {
        validTransferRequest.setToAccountId(null);
        validTransferRequest.setToIban("INVALID-IBAN");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 404 when account is not found")
    void createTransaction_shouldReturn404_whenAccountIsNotFound() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequest.class), anyString()))
                .thenThrow(new EntityNotFoundException("Account not found"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /transactions - should return 400 when a business rule is violated")
    void createTransaction_shouldReturn400_whenBusinessRuleIsViolated() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequest.class), anyString()))
                .thenThrow(new IllegalArgumentException("Insufficient funds"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    @DisplayName("POST /transactions - should return 403 when access to an account is forbidden")
    void createTransaction_shouldReturn403_whenAccessIsForbidden() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequest.class), anyString()))
                .thenThrow(new ForbiddenException("You can only transfer from your own accounts"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private TransactionResultDto buildTransactionResult(TransactionType type, double amount) {
        TransactionDto transactionDto = TransactionDto.builder()
                .transactionId(1L)
                .transactionType(type)
                .amount(MoneyDto.builder().amount(amount).currency("EUR").build())
                .channel(Channel.WEB)
                .initiatedByUserId(1L)
                .createdAt(LocalDateTime.now())
                .description("Test")
                .build();
        return TransactionResultDto.builder()
                .transaction(transactionDto)
                .sourceBalance(MoneyDto.builder().amount(900.0).currency("EUR").build())
                .build();
    }
}
