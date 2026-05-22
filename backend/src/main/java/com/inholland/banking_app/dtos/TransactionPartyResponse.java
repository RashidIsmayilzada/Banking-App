package com.inholland.banking_app.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPartyResponse {
    private Long accountId;
    private String iban;
    private Long userId;
    private String name;
}
