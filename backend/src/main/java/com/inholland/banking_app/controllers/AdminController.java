package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.EmployeeCreateRequest;
import com.inholland.banking_app.dtos.EmployeeResponse;
import com.inholland.banking_app.dtos.EmployeeUpdateRequest;
import com.inholland.banking_app.services.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import com.inholland.banking_app.dtos.TransactionReversalResponse;
import com.inholland.banking_app.dtos.AuditLogResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Endpoints for high-level system control, employee management, and auditing")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Health Check", description = "Verify that the admin API endpoints are reachable and the user has correct ADMIN authority.")
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Operation(summary = "Create an Employee", description = "Provisions a new employee profile and user account. Fails if email, username, or employee number is already taken.")
    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.ok(adminService.createEmployee(request));
    }

    @Operation(summary = "Get All Employees", description = "Retrieves a list of all provisioned employee accounts in the system.")
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(adminService.getAllEmployees());
    }

    @Operation(summary = "Get Employee by ID", description = "Retrieves the details of a specific employee profile using their system ID.")
    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getEmployee(id));
    }

    @Operation(summary = "Update Employee", description = "Updates the basic profile information (First Name, Last Name) of an existing employee.")
    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateEmployee(id, request));
    }

    @Operation(summary = "Set Employee Status", description = "Enables or disables an employee's access to the system. Useful for temporary suspensions or leaves of absence.")
    @PatchMapping("/employees/{id}/status")
    public ResponseEntity<EmployeeResponse> setEmployeeStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(adminService.setEmployeeStatus(id, active));
    }

    @Operation(summary = "Delete Employee", description = "Performs a soft delete on an employee account, disabling their access while retaining their historical data for audit purposes.")
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        adminService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get All Accounts", description = "Retrieves a system-wide list of all customer bank accounts.")
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    @Operation(summary = "Get Account by ID", description = "Retrieves the details, balances, and status of a specific bank account.")
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAccount(id));
    }

    @Operation(summary = "Freeze Account", description = "Instantly freezes a bank account, blocking all outgoing withdrawals or transfers. Used for fraud containment.")
    @PatchMapping("/accounts/{id}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.freezeAccount(id));
    }

    @Operation(summary = "Unfreeze Account", description = "Removes a freeze from a bank account, restoring normal transaction capabilities.")
    @PatchMapping("/accounts/{id}/unfreeze")
    public ResponseEntity<AccountResponse> unfreezeAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unfreezeAccount(id));
    }

    @Operation(summary = "Close Account", description = "Permanently closes a bank account. No further transactions can be processed on a closed account.")
    @PatchMapping("/accounts/{id}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.closeAccount(id));
    }

    @Operation(summary = "Reverse Transaction", description = "Reverses a previously completed transaction. Automatically creates a compensating reversal record to restore balances.")
    @PostMapping("/transactions/{id}/reverse")
    public ResponseEntity<TransactionReversalResponse> reverseTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        return ResponseEntity.ok(adminService.reverseTransaction(id, adminUsername));
    }

    @Operation(summary = "Get Audit Logs", description = "Retrieves the immutable system audit log, detailing all high-level actions taken by administrative users.")
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }

}