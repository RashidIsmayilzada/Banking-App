package com.inholland.banking_app.dtos;


import com.inholland.banking_app.models.enums.CustomerStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveCustomer {

    private Long customerId;

    private CustomerStatus status;
}
