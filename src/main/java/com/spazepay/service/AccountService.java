package com.spazepay.service;

import com.spazepay.model.Account;
import com.spazepay.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private AccountRepository accountRepository;

    public Account getAccountByUserId(Long userId) {
        logger.info("Fetching account for user ID: {}", userId);
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    logger.warn("No account found for user ID: {}", userId);
                    return new IllegalStateException("Account not found");
                });
    }

    @Transactional
    public BigDecimal topUpAccount(Long userId, BigDecimal amount) {
        Account account = getAccountByUserId(userId);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        logger.info("Account topped up for user ID: {}, new balance: {}", userId, account.getBalance());
        return account.getBalance();
    }
}