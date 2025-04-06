package com.spazepay.dto;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;


@Getter
@Data
public class SavingsPlanResponse {

    // Getters
    private Long planId;
    private String status;
    private BigDecimal principalBalance;

    public SavingsPlanResponse(Long planId, String status, BigDecimal principalBalance) {
        this.planId = planId;
        this.status = status;
        this.principalBalance = principalBalance;
    }

}
