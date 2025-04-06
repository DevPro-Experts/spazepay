package com.spazepay.controller;

import com.spazepay.dto.AccountBalanceResponse;
import com.spazepay.model.Account;
import com.spazepay.model.User;
import com.spazepay.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}