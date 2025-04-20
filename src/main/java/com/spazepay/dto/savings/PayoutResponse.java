package com.spazepay.dto.savings;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayoutResponse {
    private Long groupId;
    private Integer cycleNumber;
    private Long recipientId;
    private BigDecimal payoutAmount;
    private String message;
}