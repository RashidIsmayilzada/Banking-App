package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.AtmDepositRequest;
import com.inholland.banking_app.dtos.AtmSessionStartRequest;
import com.inholland.banking_app.dtos.AtmSessionResponse;
import com.inholland.banking_app.dtos.AtmWithdrawalRequest;
import com.inholland.banking_app.dtos.TransferResultResponse;
import com.inholland.banking_app.services.AtmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
public class AtmController {

    private final AtmService atmService;

    @PostMapping("/atm/sessions")
    public ResponseEntity<AtmSessionResponse> startSession(@Valid @RequestBody AtmSessionStartRequest request) {
        log.info("ATM session start requested for email={}", request.getEmail());
        AtmSessionResponse response = atmService.startSession(request);
        return ResponseEntity.status(201).body(response);
    }

    @DeleteMapping("/atm/sessions/{sessionId}")
    public ResponseEntity<Void> endSession(@PathVariable Long sessionId, Authentication authentication) {
        log.info("ATM session end requested for sessionId={}", sessionId);
        atmService.endSession(sessionId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/atm/transactions/deposits")
    public ResponseEntity<TransferResultResponse> deposit(@Valid @RequestBody AtmDepositRequest request,
                                                          Authentication authentication) {
        log.info("ATM deposit requested for accountId={}", request.getAccountId());
        TransferResultResponse result = atmService.deposit(request, authentication.getName());
        return ResponseEntity.status(201).body(result);
    }

    @PostMapping("/atm/transactions/withdrawals")
    public ResponseEntity<TransferResultResponse> withdraw(@Valid @RequestBody AtmWithdrawalRequest request,
                                                           Authentication authentication) {
        log.info("ATM withdrawal requested for accountId={}", request.getAccountId());
        TransferResultResponse result = atmService.withdraw(request, authentication.getName());
        return ResponseEntity.status(201).body(result);
    }
}