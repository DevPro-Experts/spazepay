package com.spazepay.dto.savings;

import lombok.Getter;

@Getter
public class LiquidateResponse {
    private String principal;
    private String interest;
    private String tax;
    private String netPayout;
    private String status;

    public LiquidateResponse(String principal, String interest, String tax, String netPayout, String status) {
        this.principal = principal;
        this.interest = interest;
        this.tax = tax;
        this.netPayout = netPayout;
        this.status = status;
    }
}