package com.inholland.banking_app.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EmployeeResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String employeeNumber;
    private boolean active;
    private boolean enabled;
    private LocalDateTime createdAt;
}
