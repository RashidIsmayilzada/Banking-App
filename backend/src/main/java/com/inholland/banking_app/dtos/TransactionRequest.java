package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransactionRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    private Long fromAccountId;

    private Long toAccountId;

    @Pattern(regexp = "^NL[0-9]{2}INHO0[0-9]{9}$", message = "Invalid IBAN format")
    private String toIban;

    private Long accountId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
