package com.spazepay.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LiquidateRequest {

    // Getters and Setters
    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotNull(message = "PIN is required")
    @Size(min = 4, max = 4, message = "PIN must be 4 digits")
    private Integer pin;

}