package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionResponse {
    private Long transactionId;
    private TransactionType transactionType;
    private MoneyResponse amount;
    private TransactionPartyResponse fromAccount;
    private TransactionPartyResponse toAccount;
    private Channel channel;
    private Long initiatedByUserId;
    private LocalDateTime createdAt;
    private String description;
}
