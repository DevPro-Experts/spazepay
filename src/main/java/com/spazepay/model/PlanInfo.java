package com.spazepay.model;

import lombok.Data;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class PlanInfo {

    private Long id;
    private String name;
    private String status;
    private String principalBalance;
    private String interestHandling;
    private Instant createdAt;
    private LocalDateTime maturedAt;
    private String type;
    private String accruedInterest;
}
