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

    // TODO: implement the makeInternalTransfer method to allow the current customer to make an internal transfer between their own accounts. The request body should contain the source account ID, destination account ID, and the amount to transfer. This should only allow transfers between accounts that belong to the current customer.
    @PostMapping("/me/transfers/internal")
    public ResponseEntity<?> makeInternalTransfer() {
        return null;
    }

    // TODO: implement the makeExternalTransfer method to allow the current customer to make an external transfer to another customer's account. The request body should contain the source account ID, destination IBAN, and the amount to transfer. This should only allow transfers from accounts that belong to the current customer.
    @PostMapping("/transfers/external")
    public ResponseEntity<?> makeExternalTransfer() {
        return null;
    }

    // TODO: implement the getMyTransactions method to return the current customer's transaction history. This should return a list of transactions for all accounts that belong to the current customer.
    @GetMapping("/me/transactions")
    public ResponseEntity<?> getMyTransactions() {
        return null;
    }
}
