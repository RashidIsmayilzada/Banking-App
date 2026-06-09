package com.inholland.banking_app.dtos;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


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
