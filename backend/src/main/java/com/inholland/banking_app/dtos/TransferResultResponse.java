package com.inholland.banking_app.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferResultResponse {
    private TransactionResponse transaction;
    private MoneyResponse sourceBalance;
    private MoneyResponse destinationBalance;
}