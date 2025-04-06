package com.spazepay.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class LiquidateResponse {

    // Getters
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal tax;
    private BigDecimal netPayout;
    private String status;

    public LiquidateResponse(BigDecimal principal, BigDecimal interest, BigDecimal tax, BigDecimal netPayout, String status) {
        this.principal = principal;
        this.interest = interest;
        this.tax = tax;
        this.netPayout = netPayout;
        this.status = status;
    }

}