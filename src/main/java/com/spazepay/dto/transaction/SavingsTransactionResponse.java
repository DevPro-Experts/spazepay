package com.spazepay.dto.transaction;

import com.spazepay.model.PlanInfo;
import lombok.Data;

import java.time.Instant;

@Data
public class SavingsTransactionResponse {

    private Long id;
    private PlanInfo plan;
    private String type;
    private String amount;
    private String source;
    private String netAmount;
    private Instant timestamp;
}
