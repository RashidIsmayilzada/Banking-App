package com.inholland.banking_app.dtos;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountSearchResult {
    private String iban;
    private String name;
}
