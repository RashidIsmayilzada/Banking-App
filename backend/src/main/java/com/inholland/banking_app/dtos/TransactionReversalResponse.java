package com.inholland.banking_app.dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionReversalResponse {

    private Long originalTransactionId;
    private Long reversalTransactionId;
    private String transactionType;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;

}