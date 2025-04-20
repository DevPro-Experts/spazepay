package com.spazepay.dto.savings;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContributionRequest {
    @NotNull(message = "Cycle number cannot be null")
    @Min(value = 1, message = "Cycle number must be at least 1")
    private Integer cycleNumber;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "PIN cannot be null")
    private String pin;
}