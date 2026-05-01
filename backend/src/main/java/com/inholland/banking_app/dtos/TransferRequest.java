package com.inholland.banking_app.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequest {
    private String fromIban;
    private String toIban;
    private Double amount;
    private String transactionType;
    private String description;


}
