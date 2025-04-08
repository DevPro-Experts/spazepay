package com.spazepay.dto.account;

import com.spazepay.util.CurrencyFormatter;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopUpAccountResponse {

    private BigDecimal newBalance;
    private String message;

    public String getFormattedNewBalance() {
        return CurrencyFormatter.formatCurrency(newBalance);
    }
    
    public TopUpAccountResponse(BigDecimal newBalance, String message) {
        this.newBalance = newBalance;
        this.message = message;
    }
}
