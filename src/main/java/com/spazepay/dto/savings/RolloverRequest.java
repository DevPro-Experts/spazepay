package com.spazepay.dto.savings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RolloverRequest {

    @NotBlank(message = "New plan name is required")
    private String newPlanName;

    @NotNull(message = "Interest handling for new plan is required")
    private String newInterestHandling;

    private LocalDate newMaturedAt; // Optional new maturity date
}