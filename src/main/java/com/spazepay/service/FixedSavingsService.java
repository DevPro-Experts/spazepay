package com.spazepay.service;

import com.spazepay.dto.savings.*;
import com.spazepay.model.*;
import com.spazepay.model.enums.SavingsType;
import com.spazepay.model.enums.TransactionType;
import com.spazepay.model.savings.FixedPlan;
import com.spazepay.repository.*;
import com.spazepay.util.CurrencyFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FixedSavingsService {
    private static final Logger logger = LoggerFactory.getLogger(FixedSavingsService.class);

    @Autowired
    private FixedPlanRepository fixedPlanRepository;

    @Autowired
    private SavingsTransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public SavingsPlanResponse createFixedPlan(User user, CreateFixedPlanRequest request) {
        if (!user.getPin().equals(request.getPin())) {
            throw new IllegalArgumentException("Invalid PIN");
        }

        Account account = accountService.getAccountByUserId(user.getId());
        if (account.getBalance().compareTo(request.getInitialDeposit()) < 0) {
            throw new IllegalStateException("Insufficient account balance");
        }

        account.setBalance(account.getBalance().subtract(request.getInitialDeposit()));
        accountRepository.save(account);

        FixedPlan plan = new FixedPlan();
        plan.setUser(user);
        plan.setName(request.getName());
        plan.setPrincipalBalance(request.getInitialDeposit());
        plan.setLockPeriodMonths(request.getLockPeriodMonths());
        plan.setMaturedAt(LocalDateTime.now().plusMonths(request.getLockPeriodMonths()));
        plan.setInterestRate(new BigDecimal("7.00"));
        fixedPlanRepository.save(plan);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlanType(SavingsType.FIXED);
        tx.setPlanId(plan.getId());
        tx.setType(TransactionType.TOPUP);
        tx.setAmount(request.getInitialDeposit());
        tx.setSource(request.getSource());
        tx.setNetAmount(request.getInitialDeposit());
        transactionRepository.save(tx);

        logger.info("Fixed plan created: {}", plan.getId());
        emailService.sendHtmlEmail(
                user.getEmail(),
                "New Fixed Savings Plan Created",
                "<p>Dear " + user.getFullName() + ",</p>" +
                        "<p>Your fixed savings plan '" + plan.getName() + "' has been created with an initial deposit of " +
                        CurrencyFormatter.formatCurrency(request.getInitialDeposit()) + ".</p>" +
                        "<p>Lock Period: " + request.getLockPeriodMonths() + " months</p>"
        );

        return new SavingsPlanResponse(
                plan.getId(),
                plan.getStatus().name(),
                CurrencyFormatter.formatCurrency(plan.getPrincipalBalance()),
                plan.getName(),
                plan.getMaturedAt(),
                plan.getCreatedAt(),
                SavingsType.FIXED,
                CurrencyFormatter.formatCurrency(plan.getAccruedInterest())
        );
    }

    public AccruedInterestResponse getAccruedInterest(User user, Long planId, AccruedInterestRequest request) {
        FixedPlan plan = fixedPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (!plan.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to plan");
        }

        Instant startInstant = request.getStartDate().toInstant(ZoneOffset.UTC);
        Instant endInstant = request.getEndDate().toInstant(ZoneOffset.UTC);

        List<SavingsTransaction> interestTransactions = transactionRepository.findByPlanIdAndTypeAndDateRange(
                SavingsType.FIXED, planId, TransactionType.INTEREST, startInstant, endInstant
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