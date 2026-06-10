package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User details returned by the API")
public class UserResponse {
    @Schema(description = "Unique user ID", example = "1")
    private Long id;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Phone number", example = "+31612345678")
    private String phoneNumber;

    @Schema(description = "BSN (citizen service number)", example = "123456789")
    private String bsn;

    @Schema(description = "User role", example = "CUSTOMER", allowableValues = {"CUSTOMER", "EMPLOYEE"})
    private Role role;

    @Schema(description = "Customer approval status", example = "PENDING_APPROVAL",
            allowableValues = {"PENDING_APPROVAL", "APPROVED", "REJECTED", "CLOSED"})
    private CustomerStatus status;

    @Schema(description = "Whether the user has at least one bank account", example = "true")
    private Boolean hasAccounts;

    @Schema(description = "Number of bank accounts", example = "2")
    private Integer accountCount;

    @Schema(description = "List of bank accounts (included when explicitly requested)")
    private List<AccountResponse> accounts;

    @Schema(description = "Timestamp when the customer profile was registered")
    private LocalDateTime registeredAt;
}
