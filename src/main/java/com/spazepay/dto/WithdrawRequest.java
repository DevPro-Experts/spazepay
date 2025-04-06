package com.spazepay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WithdrawRequest {

    // Getters and Setters
    @NotNull(message = "Plan ID is required")
    private Long planId;

    @Min(value = 1, message = "Amount must be greater than 0")
    private int amount;

    @NotNull(message = "PIN is required")
    @Size(min = 4, max = 4, message = "PIN must be 4 digits")
    private Integer pin;

}