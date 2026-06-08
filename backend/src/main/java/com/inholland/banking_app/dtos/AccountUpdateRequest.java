package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.AccountStatus;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountUpdateRequest {

    @DecimalMin(value = "0.0")
    private BigDecimal absoluteTransferLimit;

    @DecimalMin(value = "0.0")
    private BigDecimal dailyTransferLimit;

    private AccountStatus status;
}
