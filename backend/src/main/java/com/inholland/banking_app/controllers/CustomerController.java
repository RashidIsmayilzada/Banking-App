package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.CustomerResponse;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/customers")
@RestController
public class CustomerController {

    // TODO: implement the getMyProfile method to return the current customer's profile information.
    @GetMapping("/me/profile")
    public ResponseEntity<CustomerResponse> getMyProfile() {
        return null;
    }

    // TODO: implement the getMyAccounts method to return the current customer's accounts information.
    @GetMapping("/me/accounts")
    public ResponseEntity<?> getMyAccounts() {
        return null;
    }

    // TODO: implement the getAccountDetailWithID method to return the details of a specific account by its ID. This should only return the account details if the account belongs to the current customer.
    @GetMapping("/me/accounts/{accountId}")
    public ResponseEntity<?> getAccoutnDetailWithID(@PathVariable Long accountId) {
        return null;
    }

    // TODO: implement the findIbanByName method to return another customer's IBAN by their first and last name. This is just for testing purposes and should not be implemented in a real application due to privacy concerns.
    // Finds another customer's IBAN by name
    @GetMapping("/iban-search")
    public ResponseEntity<String> findIbanByName(@RequestParam String firstName,
                                                 @RequestParam String lastName) {
        return null;
    }

}
