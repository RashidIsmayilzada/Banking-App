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

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.ok(adminService.createEmployee(request));
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(adminService.getAllEmployees());
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getEmployee(id));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateEmployee(id, request));
    }

    @PatchMapping("/employees/{id}/status")
    public ResponseEntity<EmployeeResponse> setEmployeeStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(adminService.setEmployeeStatus(id, active));
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        adminService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAccount(id));
    }

    @PatchMapping("/accounts/{id}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.freezeAccount(id));
    }

    @PatchMapping("/accounts/{id}/unfreeze")
    public ResponseEntity<AccountResponse> unfreezeAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unfreezeAccount(id));
    }

    @PatchMapping("/accounts/{id}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.closeAccount(id));
    }
    @PostMapping("/transactions/{id}/reverse")
    public ResponseEntity<TransactionReversalResponse> reverseTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        return ResponseEntity.ok(adminService.reverseTransaction(id, adminUsername));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }

}