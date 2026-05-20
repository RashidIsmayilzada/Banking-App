package com.inholland.banking_app.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPageResponse {
    private List<TransactionResponse> items;
    private PageMetadataResponse page;
}
