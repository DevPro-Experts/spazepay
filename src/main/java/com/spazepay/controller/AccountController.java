package com.spazepay.controller;

import com.spazepay.dto.AccountBalanceResponse;
import com.spazepay.dto.account.TopUpAccountRequest;
import com.spazepay.dto.account.TopUpAccountResponse;
import com.spazepay.model.Account;
import com.spazepay.model.SavingsPlan;
import com.spazepay.model.SavingsTransaction;
import com.spazepay.model.User;
import com.spazepay.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    @GetMapping("/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(@AuthenticationPrincipal User user) {
        logger.info("Balance request for user: {}", user.getEmail());
        Account account = accountService.getAccountByUserId(user.getId());
        AccountBalanceResponse response = new AccountBalanceResponse(account.getAccountNumber(), account.getBalance());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/topup")
    public ResponseEntity<TopUpAccountResponse> topUpAccount(@AuthenticationPrincipal User user,
                                                             @RequestBody TopUpAccountRequest request) {
        logger.info("Top-up account request for user: {}, amount: {}", user.getEmail(), request.getAmount());
        BigDecimal newBalance = accountService.topUpAccount(user.getId(), new BigDecimal(request.getAmount()));
        return ResponseEntity.ok(new TopUpAccountResponse(newBalance, "Account topped up successfully"));
    }

}