package com.spazepay.service;

import com.spazepay.dto.*;
import com.spazepay.exception.PrematureRolloverException;
import com.spazepay.model.*;
import com.spazepay.model.enums.InterestHandling;
import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.SavingsType;
import com.spazepay.model.enums.TransactionType;
import com.spazepay.repository.*;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.chrono.ChronoLocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.hibernate.internal.CoreLogging.logger;

@Service
public class SavingsService {

    private static final Logger logger = LoggerFactory.getLogger(SavingsService.class);
    private static final int MAX_ACTIVE_PLANS = 20;
    private static final int MAX_PLANS_PER_DAY = 10;
    private static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.05");
    private static final int DAYS_IN_YEAR = 365; // 5% monthly for simplicity

    @Autowired
    private SavingsPlanRepository planRepository;

    @Autowired
    private SavingsTransactionRepository transactionRepository;

    @Autowired
    private MonthlyActivityRepository monthlyActivityRepository;

    @Autowired
    private DailyBalanceRepository dailyBalanceRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private BigDecimal calculateDailyInterestRate() {
        return ANNUAL_INTEREST_RATE.divide(new BigDecimal(DAYS_IN_YEAR), 10, BigDecimal.ROUND_DOWN);
    }

    @Transactional
    public void applyDailyInterest() {
        LocalDate today = LocalDate.now();
        LocalDate interestApplicableDate = today.minusDays(2); // Interest applies to net inflow 2 days ago

        List<SavingsPlan> activePlans = planRepository.findByStatus(PlanStatus.ACTIVE);

        for (SavingsPlan plan : activePlans) {
            if (plan.getType() != SavingsType.FLEXIBLE) continue;

            // Find the confirmed net inflow for the interestApplicableDate
            DailyBalance dailyBalance = dailyBalanceRepository.findByPlanIdAndDate(plan.getId(), interestApplicableDate)
                    .orElse(null);

            if (dailyBalance != null && dailyBalance.getNetBalance().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal principalForInterest = dailyBalance.getNetBalance();
                BigDecimal dailyInterestRate = calculateDailyInterestRate();
                BigDecimal interestAccrued = principalForInterest.multiply(dailyInterestRate).setScale(2, BigDecimal.ROUND_HALF_EVEN);

                // Add the accrued interest to the plan's principal balance
                plan.setPrincipalBalance(plan.getPrincipalBalance().add(interestAccrued));
                planRepository.save(plan);

                // Record an interest transaction
                SavingsTransaction tx = new SavingsTransaction();
                tx.setPlan(plan);
                tx.setType(TransactionType.INTEREST);
                tx.setAmount(interestAccrued);
                tx.setSource("system");
                tx.setNetAmount(interestAccrued);
                transactionRepository.save(tx);

                logger.info("Daily interest of {} applied to plan {} for date {}", interestAccrued, plan.getId(), interestApplicableDate);
            }
        }
    }

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
        plan.setMaturedAt(request.getMaturedAt());
        planRepository.save(plan);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlan(plan);
        tx.setType(TransactionType.TOPUP);
        tx.setAmount(initialDeposit);
        tx.setSource("account");
        tx.setNetAmount(initialDeposit);
        transactionRepository.save(tx);

        logger.info("Flexible plan created: {}, deducted from account balance: {}", plan.getId(), initialDeposit);
        recordDailyBalance(plan, LocalDate.now(), new BigDecimal(request.getInitialDeposit()), BigDecimal.ZERO);
        return new SavingsPlanResponse(
                        plan.getId(),
                        plan.getStatus().name(),
                        plan.getPrincipalBalance(),
                        plan.getName(),
                plan.getMaturedAt(),
                plan.getCreatedAt(),
                        plan.getType(),
                        plan.getAccruedInterest()
                );
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
        recordDailyBalance(plan, LocalDate.now(), BigDecimal.ZERO, topUpAmount);
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

        recordDailyBalance(plan, LocalDate.now(), BigDecimal.ZERO, amount);
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

        BigDecimal interest = calculateDailyInterestRate();
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

    @Transactional
    public SavingsPlanResponse rolloverFlexiblePlan(User user, Long oldPlanId, RolloverRequest request) {
        SavingsPlan oldPlan = getActivePlan(user, oldPlanId);

        if (oldPlan.getMaturedAt() == null || LocalDate.now().isBefore(ChronoLocalDate.from(oldPlan.getMaturedAt()))) {
            logger.error("Attempted to rollover plan {} before maturity date: {}", oldPlanId, oldPlan.getMaturedAt());
            throw new PrematureRolloverException("Plan cannot be rolled over before its maturity date.");
        }

        BigDecimal accruedInterest = calculateAccruedInterest(oldPlan);
        BigDecimal withdrawalTax = accruedInterest.multiply(new BigDecimal("0.10"));
        BigDecimal netInterest = accruedInterest.subtract(withdrawalTax);
        BigDecimal rolloverPrincipal = oldPlan.getPrincipalBalance().add(netInterest);

        // Create the new plan
        SavingsPlan newPlan = new SavingsPlan();
        newPlan.setUser(user);
        newPlan.setName(request.getNewPlanName());
        newPlan.setPrincipalBalance(rolloverPrincipal);
        newPlan.setInterestHandling(InterestHandling.valueOf(request.getNewInterestHandling().toUpperCase()));
        newPlan.setType(SavingsType.FLEXIBLE);
        newPlan.setMaturedAt(LocalDateTime.from(request.getNewMaturedAt()));
        planRepository.save(newPlan);

        // Update the old plan's status
        oldPlan.setStatus(PlanStatus.CLOSED);
        oldPlan.setPrincipalBalance(BigDecimal.ZERO); // Set to zero after rollover
        planRepository.save(oldPlan);

        // Record transactions for the old and new plans
        SavingsTransaction oldTx = new SavingsTransaction();
        oldTx.setPlan(oldPlan);
        oldTx.setType(TransactionType.ROLLOVER_WITHDRAWAL);
        oldTx.setAmount(oldPlan.getPrincipalBalance().add(accruedInterest));
        oldTx.setNetAmount(oldPlan.getPrincipalBalance().add(netInterest));
        oldTx.setSource("rollover");
        transactionRepository.save(oldTx);

        SavingsTransaction newTx = new SavingsTransaction();
        newTx.setPlan(newPlan);
        newTx.setType(TransactionType.ROLLOVER_DEPOSIT);
        newTx.setAmount(rolloverPrincipal);
        newTx.setNetAmount(rolloverPrincipal);
        newTx.setSource("rollover");
        transactionRepository.save(newTx);

        logger.info("Plan {} rolled over to new plan {}", oldPlanId, newPlan.getId());
        return new SavingsPlanResponse(
                        newPlan.getId(),
                        newPlan.getStatus().name(),
                        newPlan.getPrincipalBalance(),
                        newPlan.getName(),
                newPlan.getMaturedAt(),
                newPlan.getCreatedAt(),
                        newPlan.getType(),
                        newPlan.getAccruedInterest()
                );
    }

    // Implement this method to calculate the total accrued interest for a plan
    private BigDecimal calculateAccruedInterest(SavingsPlan plan) {
        BigDecimal totalInterest = BigDecimal.ZERO;
        List<SavingsTransaction> interestTransactions = transactionRepository.findByPlanAndType(plan, TransactionType.INTEREST);
        for (SavingsTransaction tx : interestTransactions) {
            totalInterest = totalInterest.add(tx.getAmount());
        }
        return totalInterest;
    }

//    private BigDecimal calculateInterest(SavingsPlan plan) {
//        // Simplified: 5% monthly interest
//        return plan.getPrincipalBalance().multiply(INTEREST_RATE);
//    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduleApplyDailyInterest() {
        logger.info("Running daily interest calculation...");
        applyDailyInterest();
    }

    public List<SavingsPlanResponseLite> getAllActivePlansLite(User user) {
        List<SavingsPlan> plans = planRepository.findByUserIdAndStatus(user.getId(), com.spazepay.model.enums.PlanStatus.ACTIVE);
        return plans.stream()
                .map(this::convertToSavingsPlanResponseLite)
                .collect(Collectors.toList());
    }

    public SavingsPlanResponseLite getPlanByIdLite(User user, Long id) {
        SavingsPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access");
        }
        return convertToSavingsPlanResponseLite(plan);
    }

    public List<SavingsTransactionResponse> getTransactionsForPlan(User user, Long planId) {
        SavingsPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (!plan.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to plan transactions");
        }

        List<SavingsTransaction> transactions = transactionRepository.findByPlan(plan);

        return transactions.stream()
                .map(this::convertToSavingsTransactionResponse)
                .collect(Collectors.toList());
    }

    private void recordDailyBalance(SavingsPlan plan, LocalDate date, BigDecimal deposit, BigDecimal withdrawal) {
        // Determine the previous day's closing balance
        DailyBalance previousBalance = dailyBalanceRepository.findTopByPlanIdAndDateLessThanOrderByDateDesc(plan.getId(), date)
                .orElse(new DailyBalance(plan, date.minusDays(1), BigDecimal.ZERO, BigDecimal.ZERO)); // Default to 0 if no previous record

        BigDecimal currentBalance = previousBalance.getClosingBalance();
        BigDecimal netInflow = deposit.subtract(withdrawal);
        BigDecimal newClosingBalance = currentBalance.add(netInflow);

        DailyBalance todayBalance = new DailyBalance();
        todayBalance.setPlan(plan);
        todayBalance.setDate(date);
        todayBalance.setNetBalance(netInflow);
        todayBalance.setClosingBalance(newClosingBalance);
        dailyBalanceRepository.save(todayBalance);
    }


    private SavingsPlanResponseLite convertToSavingsPlanResponseLite(SavingsPlan plan) {
        SavingsPlanResponseLite response = new SavingsPlanResponseLite();
        response.setId(plan.getId());
        response.setName(plan.getName());
        response.setStatus(plan.getStatus().name());
        response.setPrincipalBalance(plan.getPrincipalBalance());
        response.setInterestHandling(plan.getInterestHandling().name());
        response.setCreatedAt(plan.getCreatedAt());
        response.setMaturedAt(plan.getMaturedAt());
        response.setAccruedInterest(plan.getAccruedInterest());
        response.setType(plan.getType().name());
        return response;
    }

    private SavingsTransactionResponse convertToSavingsTransactionResponse(SavingsTransaction transaction) {
        SavingsTransactionResponse response = new SavingsTransactionResponse();
        response.setId(transaction.getId());
        response.setType(transaction.getType().name());
        response.setAmount(transaction.getAmount());
        response.setSource(transaction.getSource());
        response.setNetAmount(transaction.getNetAmount());
        response.setTimestamp(transaction.getTimestamp());

        PlanInfo planInfo = new PlanInfo();
        planInfo.setId(transaction.getPlan().getId());
        planInfo.setName(transaction.getPlan().getName());
        planInfo.setStatus(transaction.getPlan().getStatus().name());
        planInfo.setPrincipalBalance(transaction.getPlan().getPrincipalBalance());
        planInfo.setInterestHandling(transaction.getPlan().getInterestHandling().name());
        planInfo.setCreatedAt(transaction.getPlan().getCreatedAt());
        planInfo.setMaturedAt(transaction.getPlan().getMaturedAt());
        planInfo.setType(transaction.getPlan().getType().name());
        planInfo.setAccruedInterest(transaction.getPlan().getAccruedInterest());

        response.setPlan(planInfo);
        return response;
    }
}