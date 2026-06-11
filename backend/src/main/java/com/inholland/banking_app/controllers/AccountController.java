package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.AccountListResponse;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.AccountSearchResult;
import com.inholland.banking_app.dtos.AccountUpdateRequest;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
public class AccountController {

    private final AccountService accountService;

    // Lists every account (employee, optionally by user) or just the caller's own (customer).
    @GetMapping
    public ResponseEntity<AccountListResponse> listAccounts(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        AccountListResponse body = isEmployee(authentication)
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

    // Searches accounts by customer name. Created for the transaction feature
    // (employee transfers/deposits need to look an account up by name).
    @GetMapping("/search")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<AccountSearchResult>> searchAccounts(@RequestParam String name) {
        return ResponseEntity.ok(accountService.searchByCustomerName(name));
    }

    // Authorization scoping is a web-layer concern; the role is already carried
    // in the token's authorities, so there is no need to load the User entity.
    private boolean isEmployee(Authentication authentication) {
        String employeeAuthority = "ROLE_" + Role.EMPLOYEE.name();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(employeeAuthority));
    }
}
