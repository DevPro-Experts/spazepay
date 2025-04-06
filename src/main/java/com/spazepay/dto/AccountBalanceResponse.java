package com.spazepay.dto;

import java.math.BigDecimal;

public class AccountBalanceResponse {
    private String accountNumber;
    private BigDecimal balance;

    public AccountBalanceResponse(String accountNumber, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
}