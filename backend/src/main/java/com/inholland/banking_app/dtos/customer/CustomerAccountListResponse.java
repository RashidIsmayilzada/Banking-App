package com.inholland.banking_app.dtos.customer;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CustomerAccountListResponse {
    private CustomerResponse customer;
    private AccountTotalsResponse totals;
    private List<CustomerAccountResponse> accounts;
}
