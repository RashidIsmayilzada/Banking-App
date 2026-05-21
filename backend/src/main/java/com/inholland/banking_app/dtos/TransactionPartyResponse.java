package com.inholland.banking_app.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionPartyResponse {
    private Long accountId;
    private String iban;
    private String name;
    private Long userId;
}