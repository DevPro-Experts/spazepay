package com.spazepay.dto.savings;

import com.spazepay.util.CurrencyFormatter;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TopUpResponse {

    // Getters
    private String newBalance;
    private String message;

    public TopUpResponse(String newBalance, String message) {
        this.newBalance = newBalance;
        this.message = message;
    }

}