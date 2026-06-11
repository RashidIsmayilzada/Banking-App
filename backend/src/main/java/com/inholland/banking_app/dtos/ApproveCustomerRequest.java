package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Request body for approving or rejecting a customer")
public class ApproveCustomerRequest {

    @Schema(description = "New approval status for the customer",
            example = "APPROVED",
            allowableValues = {"APPROVED", "REJECTED", "CLOSED"})
    private CustomerStatus status;

    @Schema(description = "Optional absolute (minimum balance) transfer limit for the checking account. Defaults to 0.00 if omitted.",
            example = "-500.00")
    private BigDecimal checkingAbsoluteLimit;

    @Schema(description = "Optional custom daily transfer limit for the checking account. Defaults to 1000.00 if omitted.",
            example = "500.00")
    private BigDecimal checkingDailyLimit;

    @Schema(description = "Optional custom daily transfer limit for the savings account. Defaults to 5000.00 if omitted.",
            example = "3000.00")
    private BigDecimal savingsDailyLimit;
}
