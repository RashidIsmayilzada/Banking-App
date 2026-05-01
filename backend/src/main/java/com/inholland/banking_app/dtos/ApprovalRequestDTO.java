package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.ApprovalDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalRequestDTO {


    @NotNull(message = "Decision is required")
    private ApprovalDecision decision;

    private String note;

}