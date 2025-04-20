package com.spazepay.dto.savings;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
public class GroupPlanResponse {
    private Long groupId;
    private String name;
    private String status;
    private BigDecimal contributionAmount;
    private String contributionFrequency;
    private LocalDate startDate;
    private int cycleCount;
    private Instant createdAt;
    private Integer currentCycle;
    private Integer contributionsForCurrentCycle;
    private Integer totalActiveMembers;
    private BigDecimal totalContributedForCurrentCycle;
}