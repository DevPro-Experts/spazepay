package com.spazepay.controller;

import com.spazepay.dto.AccountBalanceResponse;
import com.spazepay.dto.account.TopUpAccountRequest;
import com.spazepay.dto.account.TopUpAccountResponse;
import com.spazepay.dto.transaction.TransactionResponse;
import com.spazepay.model.Account;
import com.spazepay.model.Transaction;
import com.spazepay.model.User;
import com.spazepay.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<?> viewTransactions(@AuthenticationPrincipal User user,
                                              @PathVariable Long accountId) {
        logger.info("View transactions request for account ID: {} and user: {}", accountId, user.getEmail());
        Account account = accountService.getAccountByUserId(user.getId());
        if (!account.getId().equals(accountId)) {
            logger.warn("User {} attempted to access transactions for account ID {} which does not belong to them.", user.getEmail(), accountId);
            return new ResponseEntity<>("You are not authorized to access transactions for this account.", HttpStatus.FORBIDDEN);
        }
        List<Transaction> transactions = accountService.getTransactionsForAccount(accountId);
        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(this::convertToTransactionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transactionResponses);
    }

    @GetMapping("/{accountId}/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> viewTransaction(@AuthenticationPrincipal User user,
                                                               @PathVariable Long accountId,
                                                               @PathVariable Long transactionId) {
        logger.info("View single transaction request for transaction ID {}, account ID {}, and user: {}", transactionId, accountId, user.getEmail());
        Account account = accountService.getAccountByUserId(user.getId());
        if (!account.getId().equals(accountId)) {
            logger.warn("User {} attempted to access transaction ID {} of account ID {} which does not belong to them.", user.getEmail(), transactionId, accountId);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        Optional<Transaction> transactionOptional = accountService.getTransactionByIdAndAccount(transactionId, accountId);
        return transactionOptional.map(this::convertToTransactionResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private TransactionResponse convertToTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setTransactionDate(transaction.getTransactionDate());
        return response;
    }
}