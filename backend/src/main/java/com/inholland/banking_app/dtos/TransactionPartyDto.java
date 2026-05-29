package com.inholland.banking_app.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPartyDto {
    private Long accountId;
    private String iban;
    private String name;
    private Long userId;
}
