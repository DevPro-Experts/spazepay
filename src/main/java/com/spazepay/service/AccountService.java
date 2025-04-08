package com.spazepay.service;

import com.spazepay.model.Account;
import com.spazepay.model.Transaction;
import com.spazepay.model.enums.TransactionType;
import com.spazepay.repository.AccountRepository;
import com.spazepay.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;


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

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transactionRepository.save(transaction);

        logger.info("Account topped up for user ID: {}, new balance: {}", userId, account.getBalance());
        return account.getBalance();
    }

    public List<Transaction> getTransactionsForAccount(Long accountId) {
        logger.info("Fetching transactions for account ID: {}", accountId);
        return transactionRepository.findByAccount_IdOrderByTransactionDateDesc(accountId);
    }

    public Optional<Transaction> getTransactionByIdAndAccount(Long transactionId, Long accountId) {
        logger.info("Fetching transaction ID {} for account ID: {}", transactionId, accountId);
        return transactionRepository.findByIdAndAccount_Id(transactionId, accountId);
    }
}