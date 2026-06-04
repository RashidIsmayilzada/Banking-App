package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.services.AccountService;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<AccountListResponse> listAccounts(
            @RequestParam(required = false) Long userId,
            Pageable pageable,
            Authentication authentication) {
        // Scope is decided here, not in the service: employees may list any
        // (or all) accounts; customers only ever see their own.
        AccountListResponse body = isEmployee(authentication)
                ? accountService.listAccounts(userId, pageable)
                : accountService.listAccountsOwnedBy(authentication.getName(), pageable);
        return ResponseEntity.ok(body);
    }

    // TEMP: role is detected from the authorities here as a stand-in until the
    // auth module exposes a custom principal (UserPrincipal.isEmployee()).
    // Once that lands, replace this helper with @AuthenticationPrincipal.
    private boolean isEmployee(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_EMPLOYEE"));
    }

    @GetMapping("/{iban}")
    @PostAuthorize("hasRole('EMPLOYEE') or returnObject.body.ownerUsername == authentication.name")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.getAccount(iban));
    }

    @PatchMapping("/{iban}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable String iban,
            @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(iban, request));
    }
}