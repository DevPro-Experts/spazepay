package com.spazepay.dto;

import lombok.Data;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class SavingsPlanResponseLite {

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