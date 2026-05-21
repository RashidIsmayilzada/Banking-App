package com.inholland.banking_app.dtos;


import lombok.Getter;
import lombok.Setter;

public class ApproveCustomer {

    @Getter
    @Setter

    private Long customerId;

    private String status;
}
