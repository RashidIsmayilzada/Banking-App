package com.inholland.banking_app.dtos;


import lombok.Data;


import java.time.LocalDate;
@Data
public class EmployeeUpdateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String employeeNumber;
    private boolean active;
    private boolean enabled;
    private LocalDate registeredAt;
}
