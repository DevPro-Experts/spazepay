package com.spazepay.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PlanInfo {

    private Long id;
    private String name;
    private String status;
    private BigDecimal principalBalance;
    private String interestHandling;
    private Instant createdAt;
    private LocalDateTime maturedAt;
    private String type;
    private BigDecimal accruedInterest;
}
