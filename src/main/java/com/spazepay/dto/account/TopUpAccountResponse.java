package com.spazepay.dto.account;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopUpAccountResponse {

    private BigDecimal newBalance;
    private String message;

    public TopUpAccountResponse(BigDecimal newBalance, String message) {
        this.newBalance = newBalance;
        this.message = message;
    }
}
