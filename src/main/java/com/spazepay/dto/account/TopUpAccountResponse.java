package com.spazepay.dto.account;

import com.spazepay.util.CurrencyFormatter;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TopUpAccountResponse {
    private String newBalance;
    private String message;

    public TopUpAccountResponse(String newBalance, String message) {
        this.newBalance = newBalance;
        this.message = message;
    }
}
