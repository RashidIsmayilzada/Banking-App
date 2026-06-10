package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.policies.AccountPolicy;
import com.inholland.banking_app.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Accounts", description = "View and manage customer accounts")
@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
public class AccountController {

    private final AccountService accountService;
    private final AccountPolicy accountPolicy;

    @Operation(
            summary = "List accounts",
            description = "Employees see every account (optionally filtered by userId); "
                    + "customers see only their own. Results are paginated and include the combined balance.")
    @GetMapping
    public ResponseEntity<AccountListResponse> listAccounts(
            @RequestParam(required = false) Long userId,
            Pageable pageable,
            Authentication authentication) {
        AccountListResponse body = accountPolicy.isEmployee(authentication)
                ? accountService.listAccounts(userId, pageable)
                : accountService.listAccountsOwnedBy(authentication.getName(), pageable);
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Get account details",
            description = "Returns a single account by IBAN, including the owner's personal details. "
                    + "Customers may only view their own accounts; employees may view any.")
    @GetMapping("/{iban}")
    @PostAuthorize("hasRole('EMPLOYEE') or returnObject.body.ownerUsername == authentication.name")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.getAccount(iban));
    }

    @Operation(
            summary = "Update an account (employee only)",
            description = "Updates the absolute and/or daily transfer limits, or closes the account "
                    + "by setting its status to CLOSED.")
    @PatchMapping("/{iban}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable String iban,
            @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(iban, request));
    }
}