package com.spazepay.dto.savings;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTargetPlanRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @DecimalMin(value = "1000.00", message = "Initial deposit must be at least ₦1000")
    private BigDecimal initialDeposit;

    @NotBlank(message = "Source is required")
    private String source;

    @NotBlank(message = "Pin is required")
    @Size(min = 4, max = 4, message = "Pin must be 4 digits")
    private String pin;

    @DecimalMin(value = "1000.00", message = "Target amount must be at least ₦1000")
    private BigDecimal targetAmount;

    @NotNull(message = "Target date is required")
    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    private boolean autoDebitEnabled;

    @Pattern(regexp = "WEEKLY|MONTHLY", message = "Auto-debit frequency must be WEEKLY or MONTHLY")
    private String autoDebitFrequency;
}