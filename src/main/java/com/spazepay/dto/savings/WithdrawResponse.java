package com.spazepay.dto.savings;

import lombok.Getter;

// Updated
@Getter
public class WithdrawResponse {
    private String principalBalance; // Changed to String
    private int withdrawalCount;
    private boolean interestForfeited;
    private String message;

    public WithdrawResponse(String principalBalance, int withdrawalCount, boolean interestForfeited, String message) {
        this.principalBalance = principalBalance;
        this.withdrawalCount = withdrawalCount;
        this.interestForfeited = interestForfeited;
        this.message = message;
    }
}