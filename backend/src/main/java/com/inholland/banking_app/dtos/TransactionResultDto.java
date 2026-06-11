package com.inholland.banking_app.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResultDto {
    private TransactionDto transaction;
    private MoneyResponse sourceBalance;
    private MoneyResponse destinationBalance;
}
