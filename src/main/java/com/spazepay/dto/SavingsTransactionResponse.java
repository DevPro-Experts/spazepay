package com.spazepay.dto;

import com.spazepay.model.PlanInfo;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class SavingsTransactionResponse {

    private Long id;
    private PlanInfo plan;
    private String type;
    private BigDecimal amount;
    private String source;
    private BigDecimal netAmount;
    private Instant timestamp;
}
