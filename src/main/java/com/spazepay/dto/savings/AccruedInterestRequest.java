package com.spazepay.dto.savings;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class AccruedInterestRequest {
    @NotNull
    private Long planId;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    // Getters and setters
}