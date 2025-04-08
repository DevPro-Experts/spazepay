package com.spazepay.dto.savings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TopUpRequest {

    // Getters and Setters
    @NotNull(message = "Plan ID is required")
    private Integer planId;

    @Min(value = 1, message = "Amount must be greater than 0")
    private int amount;

    @NotBlank(message = "Source is required")
    private String source;

    @NotBlank(message = "Pin is required")
    @Size(min = 4, max = 4, message = "Pin must be 4 digits")
    private String pin;

}