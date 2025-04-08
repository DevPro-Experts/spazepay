package com.spazepay.dto.account;

import lombok.Getter;

@Getter
public class AccountBalanceResponse {
    private String accountNumber;
    private String balance;

    public AccountBalanceResponse(String accountNumber, String balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }
}