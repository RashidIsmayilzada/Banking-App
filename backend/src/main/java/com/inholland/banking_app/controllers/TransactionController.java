package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.TransactionPageResponse;
import com.inholland.banking_app.dtos.TransactionRequest;
import com.inholland.banking_app.dtos.TransferResultResponse;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.services.TransactionService;
import com.inholland.banking_app.services.TransactionService.TransactionListFilters;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransferResultResponse> create(@Valid @RequestBody TransactionRequest request,
                                                         Authentication authentication) {
        log.info("Create {} transaction requested by user={}", request.getType(), authentication.getName());
        TransferResultResponse result = transactionService.create(request, authentication.getName());
        return ResponseEntity.status(201).body(result);
    }

    @GetMapping
    public ResponseEntity<TransactionPageResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) BigDecimal amountEquals,
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Channel channel,
            Pageable pageable,
            Authentication authentication) {
        TransactionListFilters filters = new TransactionListFilters(
                startDateTime, endDateTime, amountMin, amountMax, amountEquals,
                iban, userId, accountId, channel
        );
        TransactionPageResponse page = transactionService.list(filters, pageable, authentication.getName());
        return ResponseEntity.ok(page);
    }
}
