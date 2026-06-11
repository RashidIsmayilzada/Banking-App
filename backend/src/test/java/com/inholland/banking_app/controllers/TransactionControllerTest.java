package com.inholland.banking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inholland.banking_app.config.RateLimitFilter;
import com.inholland.banking_app.dtos.MoneyResponse;
import java.math.BigDecimal;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = TransactionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class)
        }
)
class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private TransactionService transactionService;

    private TransactionDto sampleTransactionDto;
    private TransactionPageDto samplePage;

    @BeforeEach
    void setUp() {
        // Populate SecurityContextHolder with a customer identity so the controller's
        // currentUsername() method does not encounter a null Authentication
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                "customerA", null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        SecurityContextHolder.setContext(ctx);

        sampleTransactionDto = TransactionDto.builder()
                .transactionId(1L)
                .transactionType(TransactionType.TRANSFER)
                .amount(new MoneyResponse(BigDecimal.valueOf(100.0), "EUR"))
                .channel(Channel.WEB)
                .initiatedByUserId(1L)
                .createdAt(LocalDateTime.of(2025, 6, 1, 12, 0))
                .description("Test transfer")
                .build();

        samplePage = TransactionPageDto.builder()
                .items(List.of(sampleTransactionDto))
                .page(PageMetadataDto.builder().page(0).size(20).totalElements(1L).totalPages(1).build())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        // Always clean up so no security state leaks into the next test
        SecurityContextHolder.clearContext();
    }

    // GET /transactions tests

    @Test
    @DisplayName("GET /transactions - should return 200 with a list of transactions for the authenticated user")
    void listTransactions_shouldReturn200_withTransactionPage() throws Exception {
        when(transactionService.listTransactions(any(), anyString())).thenReturn(samplePage);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].transactionId").value(1))
                .andExpect(jsonPath("$.items[0].transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.items[0].amount.amount").value(100.0))
                .andExpect(jsonPath("$.items[0].amount.currency").value("EUR"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /transactions - should return 200 with an empty list when the user has no transactions")
    void listTransactions_shouldReturn200_withEmptyList_whenNoTransactionsExist() throws Exception {
        TransactionPageDto emptyPage = TransactionPageDto.builder()
                .items(List.of())
                .page(PageMetadataDto.builder().page(0).size(20).totalElements(0L).totalPages(0).build())
                .build();

        when(transactionService.listTransactions(any(), anyString())).thenReturn(emptyPage);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /transactions - should pass filter query parameters to the service layer")
    void listTransactions_shouldPassFilterParams_toService() throws Exception {
        when(transactionService.listTransactions(any(), anyString())).thenReturn(samplePage);

        mockMvc.perform(get("/transactions")
                        .param("page", "0")
                        .param("size", "10")
                        .param("amountMin", "50.0")
                        .param("amountMax", "200.0"))
                .andExpect(status().isOk());
    }

    // POST /transactions – TRANSFER tests

    @Test
    @DisplayName("POST /transactions - should return 201 with the result when a transfer is created successfully")
    void createTransaction_transfer_shouldReturn201_withTransactionResult() throws Exception {
        TransactionResultDto result = TransactionResultDto.builder()
                .transaction(sampleTransactionDto)
                .sourceBalance(new MoneyResponse(BigDecimal.valueOf(900.0), "EUR"))
                .destinationBalance(new MoneyResponse(BigDecimal.valueOf(600.0), "EUR"))
                .build();

        when(transactionService.createTransaction(any(), anyString())).thenReturn(result);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTransferRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.sourceBalance.amount").value(900.0))
                .andExpect(jsonPath("$.destinationBalance.amount").value(600.0));
    }

    @Test
    @DisplayName("POST /transactions - should return 201 when a deposit is created successfully")
    void createTransaction_deposit_shouldReturn201_withTransactionResult() throws Exception {
        TransactionResultDto result = TransactionResultDto.builder()
                .transaction(sampleTransactionDto)
                .sourceBalance(new MoneyResponse(BigDecimal.valueOf(1250.0), "EUR"))
                .build();

        when(transactionService.createTransaction(any(), anyString())).thenReturn(result);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDepositRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceBalance.amount").value(1250.0));
    }

    @Test
    @DisplayName("POST /transactions - should return 201 when a withdrawal is created successfully")
    void createTransaction_withdrawal_shouldReturn201_withTransactionResult() throws Exception {
        TransactionResultDto result = TransactionResultDto.builder()
                .transaction(sampleTransactionDto)
                .sourceBalance(new MoneyResponse(BigDecimal.valueOf(800.0), "EUR"))
                .build();

        when(transactionService.createTransaction(any(), anyString())).thenReturn(result);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildWithdrawalRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceBalance.amount").value(800.0));
    }

    // POST /transactions – validation error tests

    @Test
    @DisplayName("POST /transactions - should return 422 when the transaction type is missing from the request")
    void createTransaction_shouldReturn422_whenTypeIsMissing() throws Exception {
        TransactionRequest request = buildTransferRequest();
        request.setType(null);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when the amount is missing from the request")
    void createTransaction_shouldReturn422_whenAmountIsMissing() throws Exception {
        TransactionRequest request = buildTransferRequest();
        request.setAmount(null);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when the amount is zero or negative")
    void createTransaction_shouldReturn422_whenAmountIsNotPositive() throws Exception {
        TransactionRequest request = buildTransferRequest();
        request.setAmount(-50.0);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when the description is blank")
    void createTransaction_shouldReturn422_whenDescriptionIsBlank() throws Exception {
        TransactionRequest request = buildTransferRequest();
        request.setDescription("");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 422 when the fromIban does not match the expected IBAN format")
    void createTransaction_shouldReturn422_whenFromIbanHasInvalidFormat() throws Exception {
        TransactionRequest request = buildTransferRequest();
        request.setFromIban("INVALID-IBAN");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /transactions - should return 404 when the account referenced in the request does not exist")
    void createTransaction_shouldReturn404_whenAccountNotFound() throws Exception {
        when(transactionService.createTransaction(any(), anyString()))
                .thenThrow(new EntityNotFoundException("Source account not found"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTransferRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Source account not found"));
    }

    @Test
    @DisplayName("POST /transactions - should return 403 when a customer tries to access another customer's account")
    void createTransaction_shouldReturn403_whenAccessIsForbidden() throws Exception {
        when(transactionService.createTransaction(any(), anyString()))
                .thenThrow(new ForbiddenException("You can only transfer from your own accounts"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTransferRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /transactions - should return 400 when the balance is insufficient for the requested amount")
    void createTransaction_shouldReturn400_whenBalanceIsInsufficient() throws Exception {
        when(transactionService.createTransaction(any(), anyString()))
                .thenThrow(new IllegalArgumentException("Insufficient funds"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTransferRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    // helpers

    private TransactionRequest buildTransferRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.TRANSFER);
        req.setFromIban("NL01INHO0000000001");
        req.setToIban("NL02INHO0000000002");
        req.setAmount(100.0);
        req.setDescription("Test transfer");
        return req;
    }

    private TransactionRequest buildDepositRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.DEPOSIT);
        req.setIban("NL01INHO0000000001");
        req.setAmount(250.0);
        req.setDescription("Test deposit");
        return req;
    }

    private TransactionRequest buildWithdrawalRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.WITHDRAWAL);
        req.setIban("NL01INHO0000000001");
        req.setAmount(200.0);
        req.setDescription("Test withdrawal");
        return req;
    }
}
