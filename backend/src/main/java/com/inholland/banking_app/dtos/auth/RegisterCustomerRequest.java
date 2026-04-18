package com.inholland.banking_app.dtos.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterCustomerRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String password;
    private String bsn;
    private String phoneNumber;
}
