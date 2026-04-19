package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.customer.CustomerAccountListResponse;
import com.inholland.banking_app.dtos.customer.CustomerResponse;
import com.inholland.banking_app.services.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/customers")
@RestController
public class CustomerController {

    private final CustomerProfileService customerProfileService;

    // TODO: Test the api endpoint
    @GetMapping("/me/profile")
    public ResponseEntity<CustomerResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(customerProfileService.getCurrentCustomerProfile(requireAuthenticatedUsername(authentication)));
    }

    // TODO: implement the getMyAccounts method to return the current customer's accounts information.
    @GetMapping("/me/accounts")
    public ResponseEntity<CustomerAccountListResponse> getMyAccounts(Authentication authentication) {
        return ResponseEntity.ok(customerProfileService.getCurrentCustomerAccounts(requireAuthenticatedUsername(authentication)));
    }

    // TODO: implement the getAccountDetailWithID method to return the details of a specific account by its ID. This should only return the account details if the account belongs to the current customer.
    @GetMapping("/me/accounts/{accountId}")
    public ResponseEntity<?> getAccountDetailWithID(@PathVariable Long accountId) {
        return null;
    }

    // TODO: implement the findIbanByName method to return another customer's IBAN by their first and last name. This is just for testing purposes and should not be implemented in a real application due to privacy concerns.
    // Finds another customer's IBAN by name
    @GetMapping("/iban-search")
    public ResponseEntity<String> findIbanByName(@RequestParam String firstName,
                                                 @RequestParam String lastName) {
        return null;
    }

    private String requireAuthenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new BadCredentialsException("Authenticated user not found");
        }

        return authentication.getName();
    }

}
