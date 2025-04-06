package com.spazepay.service;

import com.spazepay.model.Account;
import com.spazepay.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}