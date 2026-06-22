package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
@Tag(name = "Account Management", description = "Endpoints for reading and managing bank accounts")
public class AccountController {

    private final AccountService accountService;

    // Lists every account (employee, optionally by user) or just the caller's own (customer).
    @GetMapping
    public ResponseEntity<AccountListResponse> listAccounts(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        boolean employee = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        AccountListResponse body = employee
                ? accountService.listAccounts(userId, pageable)
                : accountService.listAccountsOwnedBy(authentication.getName(), pageable);
        return ResponseEntity.ok(body);
    }

    // Returns one account by IBAN; customers may only read their own, employees any.
    @GetMapping("/{iban}")
    @PostAuthorize("hasRole('EMPLOYEE') or returnObject.body.ownerUsername == authentication.name")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.getAccount(iban));
    }

    // Updates transfer limits or closes the account (employee only).
    @PatchMapping("/{iban}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable String iban,
            @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(iban, request));
    }

    // --Efe(Admin)
    @Operation(summary = "Freeze Account",
            description = "Admin operation: instantly freezes a bank account, blocking all outgoing withdrawals or transfers. Used for fraud containment. Requires ADMIN role.")
    @PatchMapping("/{iban}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.freezeAccount(iban));
    }

    // --Efe(Admin)
    @Operation(summary = "Unfreeze Account",
            description = "Admin operation: removes a freeze from a bank account, restoring normal transaction capabilities. Requires ADMIN role.")
    @PatchMapping("/{iban}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> unfreezeAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.unfreezeAccount(iban));
    }

    // --Efe(Admin)
    @Operation(summary = "Close Account",
            description = "Admin operation: permanently closes a bank account. No further transactions can be processed on a closed account. Requires ADMIN role.")
    @PatchMapping("/{iban}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.closeAccount(iban));
    }
}
