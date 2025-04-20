package com.spazepay.dto.savings;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayoutRequest {
    private BigDecimal amount;
    private String pin;
    @NotNull(message = "Cycle number is required")
    private Integer cycleNumber;
}