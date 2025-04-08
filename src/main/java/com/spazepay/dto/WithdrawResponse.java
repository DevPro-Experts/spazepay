package com.spazepay.dto;

import com.spazepay.util.CurrencyFormatter;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class WithdrawResponse {

    // Getters
    private BigDecimal newBalance;
    private int withdrawalCount;
    private boolean interestForfeited;
    private String message;

    public String getFormattedNewBalance() {
        return CurrencyFormatter.formatCurrency(newBalance);
    }

    public WithdrawResponse(BigDecimal newBalance, int withdrawalCount, boolean interestForfeited, String message) {
        this.newBalance = newBalance;
        this.withdrawalCount = withdrawalCount;
        this.interestForfeited = interestForfeited;
        this.message = message;
    }

}