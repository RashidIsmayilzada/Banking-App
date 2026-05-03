package com.inholland.banking_app.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterEmployeeRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String password;
    private String employeeNumber;
}
