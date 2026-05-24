package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Unified transaction request. Required fields vary by type:
 * - TRANSFER: fromAccountId + (toAccountId OR toIban)
 * - DEPOSIT / WITHDRAWAL: accountId
 */
@Getter
@Setter
public class TransactionRequest {

    @NotNull
    private TransactionType type;

    private Long fromAccountId;

    private Long toAccountId;

    @Pattern(regexp = "^NL[0-9]{2}INHO0[0-9]{9}$", message = "toIban must match the INHO IBAN format")
    private String toIban;

    private Long accountId;

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 255)
    private String description;
}
