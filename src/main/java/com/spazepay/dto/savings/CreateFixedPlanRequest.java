package com.spazepay.dto.savings;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateFixedPlanRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @DecimalMin(value = "10000.00", message = "Initial deposit must be at least ₦10000")
    private BigDecimal initialDeposit;

    @NotBlank(message = "Source is required")
    private String source;

    @NotBlank(message = "Pin is required")
    @Size(min = 4, max = 4, message = "Pin must be 4 digits")
    private String pin;

    @Min(value = 3, message = "Lock period must be at least 3 months")
    private int lockPeriodMonths;
}