package com.inholland.banking_app.dtos.customer;

import com.inholland.banking_app.models.enums.CustomerStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerResponse {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String phoneNumber;
    private CustomerStatus status;
    private LocalDateTime registeredAt;
}
