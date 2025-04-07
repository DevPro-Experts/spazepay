package com.spazepay.dto;

import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.SavingsType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;


@Getter
@Data
public class SavingsPlanResponse {

    // Getters
    private Long planId;
    private String status;
    private BigDecimal principalBalance;
    private String name;

    private Instant createdAt;
    private LocalDateTime maturedAt;
    private SavingsType type;
    private BigDecimal accruedInterest;

    public SavingsPlanResponse(Long planId, String status, BigDecimal principalBalance, String name, LocalDateTime maturedAt, Instant createdAt, SavingsType type, BigDecimal accruedInterest) {
        this.planId = planId;
        this.status = status;
        this.principalBalance = principalBalance;
        this.name = name;
        this.maturedAt = maturedAt;
        this.createdAt = createdAt;
        this.type = type;
        this.accruedInterest = accruedInterest;
    }
}
