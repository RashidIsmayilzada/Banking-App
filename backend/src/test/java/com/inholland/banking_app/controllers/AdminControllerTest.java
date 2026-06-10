package com.inholland.banking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inholland.banking_app.dtos.*;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.security.JwtAuthenticationFilter;
import com.inholland.banking_app.services.AdminService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;



import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = AdminController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class AdminControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EmployeeResponse employeeResponse;
    private AccountResponse accountResponse;
    private EmployeeCreateRequest validEmployeeRequest;

    @BeforeEach
    void setUp() {
        employeeResponse = new EmployeeResponse();
        employeeResponse.setId(1L);
        employeeResponse.setFirstName("Test");
        employeeResponse.setLastName("Admin");
        employeeResponse.setEmail("test@bank.com");
        employeeResponse.setEmployeeNumber("EMP001");
        employeeResponse.setActive(true);

        accountResponse = AccountResponse.builder()
                .accountId(10L)
                .iban("NL01INHO0000000010")
                .balance(MoneyResponse.eur(new BigDecimal("1000.00")))
                .status(AccountStatus.ACTIVE)
                .build();


    }

    @Test
    @DisplayName("GET /admin/ping - should return 200 ok")
    void ping() throws Exception {
        mockMvc.perform(get("/admin/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("POST /admin/employees - should return 200 on success")
    void createEmployee() throws Exception {
        when(adminService.createEmployee(any(EmployeeCreateRequest.class))).thenReturn(employeeResponse);

        mockMvc.perform(post("/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmployeeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@bank.com"));
    }

    @Test
    @DisplayName("POST /admin/employees - should return 409 on duplicate")
    void createEmployee_duplicate() throws Exception {

        when(adminService.createEmployee(any(EmployeeCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("Email taken"));

        mockMvc.perform(post("/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmployeeRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /admin/employees - should return list")
    void getAllEmployees() throws Exception {
        when(adminService.getAllEmployees()).thenReturn(List.of(employeeResponse));

        mockMvc.perform(get("/admin/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /admin/employees/{id} - should return employee")
    void getEmployee() throws Exception {
        when(adminService.getEmployee(1L)).thenReturn(employeeResponse);

        mockMvc.perform(get("/admin/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /admin/employees/{id} - should update")
    void updateEmployee() throws Exception {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Updated");

        when(adminService.updateEmployee(eq(1L), any())).thenReturn(employeeResponse);

        mockMvc.perform(put("/admin/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /admin/employees/{id}/status - should update status")
    void setEmployeeStatus() throws Exception {
        when(adminService.setEmployeeStatus(eq(1L), anyBoolean())).thenReturn(employeeResponse);

        mockMvc.perform(patch("/admin/employees/1/status").param("active", "false"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /admin/employees/{id} - should return 204")
    void deleteEmployee() throws Exception {
        doNothing().when(adminService).deleteEmployee(1L);

        mockMvc.perform(delete("/admin/employees/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /admin/accounts - should return list")
    void getAllAccounts() throws Exception {
        when(adminService.getAllAccounts()).thenReturn(List.of(accountResponse));

        mockMvc.perform(get("/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /admin/accounts/{id} - should return 200")
    void getAccount() throws Exception {
        when(adminService.getAccount(10L)).thenReturn(accountResponse);

        mockMvc.perform(get("/admin/accounts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(10));
    }

    @Test
    @DisplayName("PATCH /admin/accounts/{id}/freeze - should return 200")
    void freezeAccount() throws Exception {
        when(adminService.freezeAccount(10L)).thenReturn(accountResponse);

        mockMvc.perform(patch("/admin/accounts/10/freeze"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /admin/accounts/{id}/freeze - already frozen should return 409")
    void freezeAccount_conflict() throws Exception {
        when(adminService.freezeAccount(10L)).thenThrow(new AccountStateException("Already frozen"));

        mockMvc.perform(patch("/admin/accounts/10/freeze"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /admin/accounts/{id}/unfreeze - should return 200")
    void unfreezeAccount() throws Exception {
        when(adminService.unfreezeAccount(10L)).thenReturn(accountResponse);

        mockMvc.perform(patch("/admin/accounts/10/unfreeze"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /admin/accounts/{id}/close - should return 200")
    void closeAccount() throws Exception {
        when(adminService.closeAccount(10L)).thenReturn(accountResponse);

        mockMvc.perform(patch("/admin/accounts/10/close"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /admin/transactions/{id}/reverse - should return 200")
    void reverseTransaction() throws Exception {
        TransactionReversalResponse response = new TransactionReversalResponse();
        response.setReversalTransactionId(101L);

        when(adminService.reverseTransaction(eq(100L), anyString())).thenReturn(response);

        // Must supply a principal — security is excluded but the controller calls
        // authentication.getName(), which throws NPE if authentication is null
        mockMvc.perform(post("/admin/transactions/100/reverse")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reversalTransactionId").value(101));
    }

    @Test
    @DisplayName("GET /admin/audit-logs - should return list")
    void getAuditLogs() throws Exception {
        AuditLogResponse log = new AuditLogResponse();
        log.setId(1L);
        log.setActorUsername("admin");

        when(adminService.getAuditLogs()).thenReturn(List.of(log));

        mockMvc.perform(get("/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actorUsername").value("admin"));
    }
}