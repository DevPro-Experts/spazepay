package com.spazepay.dto.savings;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContributionResponse {
    private Long contributionId;
    private Long groupId;
    private BigDecimal amount;
    private BigDecimal totalContributedForCycle;
    private String message;
}