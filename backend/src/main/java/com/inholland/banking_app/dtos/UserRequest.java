package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.Role;
import lombok.Getter;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Request body for registering a new user")
public class UserRequest {

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "User's email address", example = "john@example.com")
    private String email;

    @Schema(description = "Chosen username", example = "john_doe")
    private String username;

    @Schema(description = "Password (min 8 chars)", example = "Secure@123")
    private String password;

    @Schema(description = "User role", example = "CUSTOMER", allowableValues = {"CUSTOMER", "EMPLOYEE"})
    private Role role;

    // user data base on role
    @Schema(description = "BSN (citizen service number), required for customers", example = "123456789")
    private String bsn;

    @Schema(description = "Phone number, required for customers", example = "+31612345678")
    private String phoneNumber;

    @Schema(description = "Employee number, required for employees", example = "EMP-001")
    private String employeeNumber;

}
