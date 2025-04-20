package com.spazepay.dto.savings;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateGroupPlanRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @DecimalMin(value = "1000.00", message = "Contribution amount must be at least ₦1000")
    private BigDecimal contributionAmount;

    @Pattern(regexp = "WEEKLY|MONTHLY", message = "Frequency must be WEEKLY or MONTHLY")
    private String contributionFrequency;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @Min(value = 6, message = "Cycle count must be at least 6")
    private int cycleCount;

    private BigDecimal initialContribution;
    private String pin;
}