package com.spazepay.dto;

import com.spazepay.util.CurrencyFormatter;
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

    public String getFormattedPrincipalBalance() {
        return CurrencyFormatter.formatCurrency(principal);
    }

    public String getFormattedInterest() {
        return CurrencyFormatter.formatCurrency(interest);
    }

    public String getFormattedTax() {
        return CurrencyFormatter.formatCurrency(tax);
    }

    public String getFormattedPayout() {
        return CurrencyFormatter.formatCurrency(netPayout);
    }

    public LiquidateResponse(BigDecimal principal, BigDecimal interest, BigDecimal tax, BigDecimal netPayout, String status) {
        this.principal = principal;
        this.interest = interest;
        this.tax = tax;
        this.netPayout = netPayout;
        this.status = status;
    }

}