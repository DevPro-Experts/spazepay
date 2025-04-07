package com.spazepay.service;

import com.spazepay.dto.*;
import com.spazepay.model.*;
import com.spazepay.model.enums.InterestHandling;
import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.SavingsType;
import com.spazepay.model.enums.TransactionType;
import com.spazepay.repository.AccountRepository;
import com.spazepay.repository.MonthlyActivityRepository;
import com.spazepay.repository.SavingsPlanRepository;
import com.spazepay.repository.SavingsTransactionRepository;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.hibernate.internal.CoreLogging.logger;

@Service
public class SavingsService {

    private static final Logger logger = LoggerFactory.getLogger(SavingsService.class);
    private static final int MAX_ACTIVE_PLANS = 20;
    private static final int MAX_PLANS_PER_DAY = 10;
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.05"); // 5% monthly for simplicity

    @Autowired
    private SavingsPlanRepository planRepository;

    @Autowired
    private SavingsTransactionRepository transactionRepository;

    @Autowired
    private MonthlyActivityRepository monthlyActivityRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Transactional
    public SavingsPlanResponse createFlexiblePlan(User user, CreateFlexiblePlanRequest request) {
        long activePlans = planRepository.countByUserIdAndStatus(user.getId(), PlanStatus.ACTIVE);
        if (activePlans >= MAX_ACTIVE_PLANS) {
            throw new IllegalStateException("Maximum active plans limit reached");
        }

        BigDecimal initialDeposit = new BigDecimal(request.getInitialDeposit());
        // Validate and convert initial deposit
        if (initialDeposit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial deposit must be greater than zero");
        }
        Account account = accountService.getAccountByUserId(user.getId());
        if (account.getBalance().compareTo(initialDeposit) < 0) {
            throw new IllegalStateException("Insufficient account balance");
        }

        // Deduct from account balance
        account.setBalance(account.getBalance().subtract(initialDeposit));
        accountRepository.save(account);

        SavingsPlan plan = new SavingsPlan();
        plan.setUser(user);
        plan.setName(request.getName());
        plan.setPrincipalBalance(initialDeposit);
        plan.setInterestHandling(InterestHandling.valueOf(request.getInterestHandling().toUpperCase()));
        plan.setType(SavingsType.FLEXIBLE);
        planRepository.save(plan);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlan(plan);
        tx.setType(TransactionType.TOPUP);
        tx.setAmount(initialDeposit);
        tx.setSource("account");
        tx.setNetAmount(initialDeposit);
        transactionRepository.save(tx);

        logger.info("Flexible plan created: {}, deducted from account balance: {}", plan.getId(), initialDeposit);
        return new SavingsPlanResponse(plan.getId(), plan.getStatus().name(), plan.getPrincipalBalance());
    }


    @Transactional
    public TopUpResponse topUpFlexiblePlan(User user, TopUpRequest request) {

        if (!user.getPin().equals(request.getPin())) {
            throw new IllegalArgumentException("Invalid PIN");
        }

        Integer planId = request.getPlanId();
        if (planId == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }

        SavingsPlan plan = getActivePlan(user, Long.valueOf(request.getPlanId()));
        plan.setPrincipalBalance(plan.getPrincipalBalance().add(new BigDecimal(request.getAmount())));
        planRepository.save(plan);

        BigDecimal topUpAmount = new BigDecimal(request.getAmount());
        // Validate and convert initial deposit
        if (topUpAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial deposit must be greater than zero");
        }
        Account account = accountService.getAccountByUserId(user.getId());
        if (account.getBalance().compareTo(topUpAmount) < 0) {
            throw new IllegalStateException("Insufficient account balance");
        }

        // Deduct from account balance
        account.setBalance(account.getBalance().subtract(topUpAmount));
        accountRepository.save(account);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlan(plan);
        tx.setType(TransactionType.TOPUP);
        tx.setAmount(new BigDecimal(request.getAmount()));
        tx.setSource(request.getSource());
        tx.setNetAmount(new BigDecimal(request.getAmount()));
        transactionRepository.save(tx);

        logger.info("Top-up successful for plan: {}", plan.getId());
        return new TopUpResponse(plan.getPrincipalBalance(), "Top-up successful");
    }

    @Transactional
    public WithdrawResponse withdrawFlexiblePlan(User user, WithdrawRequest request) {
        SavingsPlan plan = getActivePlan(user, request.getPlanId());
        String currentMonth = YearMonth.now().toString(); // e.g., "2025-04"
        MonthlyActivity activity = monthlyActivityRepository.findByPlanIdAndMonth(plan.getId(), currentMonth)
                .orElse(new MonthlyActivity());

        if (activity.getId() == null) {
            activity.setUser(user);
            activity.setPlan(plan);
            activity.setMonth(currentMonth);
            activity.setWithdrawalCount(0);
            activity.setInterestForfeited(false);
        }

        int newCount = activity.getWithdrawalCount() + 1;
        activity.setWithdrawalCount(newCount);
        if (newCount > 4) {
            activity.setInterestForfeited(true);
        }
        monthlyActivityRepository.save(activity);

        BigDecimal amount = new BigDecimal(request.getAmount());
        if (plan.getPrincipalBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        plan.setPrincipalBalance(plan.getPrincipalBalance().subtract(amount));
        planRepository.save(plan);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlan(plan);
        tx.setType(TransactionType.WITHDRAWAL);
        tx.setAmount(amount);
        tx.setSource("wallet");
        tx.setNetAmount(amount);
        transactionRepository.save(tx);

        // Credit the payout to the user's account
        Account account = accountService.getAccountByUserId(user.getId());
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        logger.info("Withdrawal from plan: {}, count: {}", plan.getId(), newCount);
        return new WithdrawResponse(plan.getPrincipalBalance(), newCount, activity.isInterestForfeited(),
                "Withdrawal successful" + (activity.isInterestForfeited() ? ". Monthly interest will be forfeited." : ""));
    }

    @Transactional
    public LiquidateResponse liquidateFlexiblePlan(User user, LiquidateRequest request) {
        SavingsPlan plan = getActivePlan(user, request.getPlanId());
        String currentMonth = YearMonth.now().toString();
        MonthlyActivity activity = monthlyActivityRepository.findByPlanIdAndMonth(plan.getId(), currentMonth)
                .orElse(new MonthlyActivity());

        BigDecimal interest = calculateInterest(plan);
        BigDecimal tax = interest.multiply(new BigDecimal("0.10")); // 10% tax
        BigDecimal netInterest = interest.subtract(tax);
        BigDecimal payout = plan.getPrincipalBalance().add(netInterest);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlan(plan);
        tx.setType(TransactionType.LIQUIDATION);
        tx.setAmount(payout);
        tx.setSource("wallet");
        tx.setNetAmount(payout);
        transactionRepository.save(tx);

        plan.setStatus(PlanStatus.CLOSED);
        plan.setPrincipalBalance(BigDecimal.ZERO);
        planRepository.save(plan);

        // Credit the payout to the user's account
        Account account = accountService.getAccountByUserId(user.getId());
        account.setBalance(account.getBalance().add(payout));
        accountRepository.save(account);

        logger.info("Plan liquidated: {}", plan.getId());
        return new LiquidateResponse(plan.getPrincipalBalance(), interest, tax, payout, plan.getStatus().name());
    }

    private SavingsPlan getActivePlan(User user, @NotNull Long planId) {
        SavingsPlan plan = planRepository.findById(Long.valueOf(planId))
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        if (!plan.getUser().getId().equals(user.getId()) || plan.getStatus() != PlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Invalid or inactive plan");
        }
        return plan;
    }

    private BigDecimal calculateInterest(SavingsPlan plan) {
        // Simplified: 5% monthly interest
        return plan.getPrincipalBalance().multiply(INTEREST_RATE);
    }

    public List<SavingsPlan> getAllActivePlans(User user) {
        return planRepository.findByUserIdAndStatus(user.getId(), PlanStatus.ACTIVE);
    }

    public SavingsPlan getPlanById(User user, Long id) {
        SavingsPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access");
        }
        return plan;
    }

    public List<SavingsTransaction> getTransactionsForPlan(User user, Long planId) {
        SavingsPlan plan = getPlanById(user, planId);
        return transactionRepository.findByPlan(plan);
    }

}