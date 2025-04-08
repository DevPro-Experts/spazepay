package com.spazepay.dto;

import com.spazepay.util.CurrencyFormatter;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class AccountBalanceResponse {
    private String accountNumber;
    private BigDecimal balance;

    public String getFormattedBalance() {
        return CurrencyFormatter.formatCurrency(balance);
    }

    public AccountBalanceResponse(String accountNumber, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

}