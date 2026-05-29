package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long transactionId;
    private TransactionType transactionType;
    private MoneyDto amount;
    private TransactionPartyDto fromAccount;
    private TransactionPartyDto toAccount;
    private Channel channel;
    private Long initiatedByUserId;
    private LocalDateTime createdAt;
    private String description;
}
