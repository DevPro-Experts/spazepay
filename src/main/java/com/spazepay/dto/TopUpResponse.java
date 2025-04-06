package com.spazepay.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TopUpResponse {

    // Getters
    private BigDecimal newBalance;
    private String message;

    public TopUpResponse(BigDecimal newBalance, String message) {
        this.newBalance = newBalance;
        this.message = message;
    }

}