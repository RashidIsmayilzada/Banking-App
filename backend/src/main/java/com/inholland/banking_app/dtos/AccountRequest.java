package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "IBAN is required")
    @Size(min = 18, max = 34, message = "IBAN must be between 18 and 34 characters")
    private String iban;

    @NotNull(message = "Account type must be specified")
    private AccountType accountType;

    @NotNull(message = "Absolute transfer limit is required")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal absoluteTransferLimit;

    @NotNull(message = "Daily transfer limit is required")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal dailyTransferLimit;

    private boolean active = true;
}

