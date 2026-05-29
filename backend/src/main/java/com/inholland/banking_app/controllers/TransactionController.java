package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.TransactionFilterParams;
import com.inholland.banking_app.dtos.TransactionPageDto;
import com.inholland.banking_app.dtos.TransactionRequest;
import com.inholland.banking_app.dtos.TransactionResultDto;
import com.inholland.banking_app.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<TransactionPageDto> listTransactions(@ModelAttribute TransactionFilterParams params) {
        // Returns a paginated and filtered list of transactions for the current user
        return ResponseEntity.ok(transactionService.listTransactions(params, currentUsername()));
    }

    @PostMapping
    public ResponseEntity<TransactionResultDto> createTransaction(@Valid @RequestBody TransactionRequest request) {
        // Creates a new transaction and returns the result with updated account balances
        TransactionResultDto result = transactionService.createTransaction(request, currentUsername());
        log.info("Transaction created: type={}, initiatedBy={}", request.getType(), currentUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    private String currentUsername() {
        // Extracts the authenticated username from the security context
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
