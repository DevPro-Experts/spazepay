package com.spazepay.dto;

import com.spazepay.util.CurrencyFormatter;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TopUpResponse {

    // Getters
    private BigDecimal newBalance;
    private String message;

    public String getFormattedNewBalance() {
        return CurrencyFormatter.formatCurrency(newBalance);
    }

    public TopUpResponse(BigDecimal newBalance, String message) {
        this.newBalance = newBalance;
        this.message = message;
    }

}