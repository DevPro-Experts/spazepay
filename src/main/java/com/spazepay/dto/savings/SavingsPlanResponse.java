package com.spazepay.dto.savings;

import com.spazepay.model.enums.SavingsType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;


@Getter
@Setter
@Data
public class SavingsPlanResponse {

    // Getters
    private Long planId;
    private String status;
    private String principalBalance;
    private String name;

    private Instant createdAt;
    private LocalDateTime maturedAt;
    private SavingsType type;
    private String accruedInterest;

    public SavingsPlanResponse(Long planId, String status, String principalBalance, String name, LocalDateTime maturedAt, Instant createdAt, SavingsType type, String accruedInterest) {
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
