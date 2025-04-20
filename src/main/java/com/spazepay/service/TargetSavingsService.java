package com.spazepay.service;

import com.spazepay.dto.savings.*;
import com.spazepay.exception.SavingsException;
import com.spazepay.model.*;
import com.spazepay.model.enums.SavingsType;
import com.spazepay.model.enums.TransactionType;
import com.spazepay.model.savings.TargetPlan;
import com.spazepay.repository.*;
import com.spazepay.util.CurrencyFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TargetSavingsService {
    private static final Logger logger = LoggerFactory.getLogger(TargetSavingsService.class);

    @Autowired
    private TargetPlanRepository targetPlanRepository;

    @Autowired
    private SavingsTransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public SavingsPlanResponse createTargetPlan(User user, CreateTargetPlanRequest request) {
        if (!user.getPin().equals(request.getPin())) {
            throw new IllegalArgumentException("Invalid PIN");
        }

        Account account = accountService.getAccountByUserId(user.getId());
        if (account.getBalance().compareTo(request.getInitialDeposit()) < 0) {
            throw new IllegalStateException("Insufficient account balance");
        }

        account.setBalance(account.getBalance().subtract(request.getInitialDeposit()));
        accountRepository.save(account);

        TargetPlan plan = new TargetPlan();
        plan.setUser(user);
        plan.setName(request.getName());
        plan.setPrincipalBalance(request.getInitialDeposit());
        plan.setTargetAmount(request.getTargetAmount());
        plan.setTargetDate(request.getTargetDate());
        plan.setAutoDebitEnabled(request.isAutoDebitEnabled());
        plan.setAutoDebitFrequency(request.getAutoDebitFrequency());
        targetPlanRepository.save(plan);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlanType(SavingsType.TARGET);
        tx.setPlanId(plan.getId());
        tx.setType(TransactionType.TOPUP);
        tx.setAmount(request.getInitialDeposit());
        tx.setSource(request.getSource());
        tx.setNetAmount(request.getInitialDeposit());
        transactionRepository.save(tx);

        logger.info("Target plan created: {}", plan.getId());
        emailService.sendHtmlEmail(
                user.getEmail(),
                "New Target Savings Plan Created",
                "<p>Dear " + user.getFullName() + ",</p>" +
                        "<p>Your target savings plan '" + plan.getName() + "' has been created with an initial deposit of " +
                        CurrencyFormatter.formatCurrency(request.getInitialDeposit()) + ".</p>" +
                        "<p>Target Amount: " + CurrencyFormatter.formatCurrency(request.getTargetAmount()) + "</p>" +
                        "<p>Target Date: " + request.getTargetDate() + "</p>"
        );

        return new SavingsPlanResponse(
                plan.getId(),
                plan.getStatus().name(),
                CurrencyFormatter.formatCurrency(plan.getPrincipalBalance()),
                plan.getName(),
                null,
                plan.getCreatedAt(),
                SavingsType.TARGET,
                CurrencyFormatter.formatCurrency(plan.getAccruedInterest())
        );
    }

    public AccruedInterestResponse getAccruedInterest(User user, Long planId, AccruedInterestRequest request) {
        TargetPlan plan = targetPlanRepository.findById(planId)
                .orElseThrow(() -> new SavingsException("PLAN_NOT_FOUND", "Plan not found"));

        if (!plan.getUser().getId().equals(user.getId())) {
            throw new SavingsException("NOT_AUTHORIZED", "Unauthorized access to plan transactions");
        }

        Instant startInstant = request.getStartDate().toInstant(ZoneOffset.UTC);
        Instant endInstant = request.getEndDate().toInstant(ZoneOffset.UTC);

        List<SavingsTransaction> interestTransactions = transactionRepository.findByPlanIdAndTypeAndDateRange(
                SavingsType.TARGET, planId, TransactionType.INTEREST, startInstant, endInstant
        );

        BigDecimal totalAccruedInterest = interestTransactions.stream()
                .map(SavingsTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String startDateStr = formatter.format(request.getStartDate());
        String endDateStr = formatter.format(request.getEndDate());

        return new AccruedInterestResponse(
                planId.toString(),
                CurrencyFormatter.formatCurrency(totalAccruedInterest),
                startDateStr,
                endDateStr
        );
    }
}