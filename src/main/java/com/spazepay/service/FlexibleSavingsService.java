package com.spazepay.service;

import com.spazepay.dto.savings.*;
import com.spazepay.dto.transaction.SavingsTransactionResponse;
import com.spazepay.exception.PrematureRolloverException;
import com.spazepay.exception.SavingsException;
import com.spazepay.model.*;
import com.spazepay.model.enums.*;
import com.spazepay.model.savings.FlexiblePlan;
import com.spazepay.repository.*;
import com.spazepay.util.CurrencyFormatter;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlexibleSavingsService {

    private static final Logger logger = LoggerFactory.getLogger(FlexibleSavingsService.class);

    @Autowired
    private FlexiblePlanRepository planRepository;

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

    @Autowired
    private TransactionRepository accountTransactionRepository;

    @Autowired
    private EmailService emailService;

    private static final int MAX_ACTIVE_PLANS = 20;
    private static final int MAX_PLANS_PER_DAY = 10;

    @Transactional
    public SavingsPlanResponse createFlexiblePlan(User user, CreateFlexiblePlanRequest request) {
        long activePlans = planRepository.countByUserIdAndStatus(user.getId(), PlanStatus.ACTIVE);
        if (activePlans >= MAX_ACTIVE_PLANS) {
            throw new SavingsException("MAX_PLANS_REACHED", "Maximum active plans limit reached");
        }

        BigDecimal initialDeposit = new BigDecimal(request.getInitialDeposit());
        if (initialDeposit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial deposit must be greater than zero");
        }
        Account account = accountService.getAccountByUserId(user.getId());
        if (account.getBalance().compareTo(initialDeposit) < 0) {
            throw new SavingsException("INSUFFICIENT_BALANCE", "Insufficient account balance");
        }

        account.setBalance(account.getBalance().subtract(initialDeposit));
        accountRepository.save(account);

        FlexiblePlan plan = new FlexiblePlan();
        plan.setUser(user);
        plan.setName(request.getName());
        plan.setPrincipalBalance(initialDeposit);
        plan.setInterestHandling(InterestHandling.valueOf(request.getInterestHandling().toUpperCase()));
        plan.setMaturedAt(request.getMaturedAt());
        planRepository.save(plan);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(initialDeposit);
        accountTransactionRepository.save(transaction);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlanType(SavingsType.FLEXIBLE);
        tx.setPlanId(plan.getId());
        tx.setType(TransactionType.TOPUP);
        tx.setAmount(initialDeposit);
        tx.setSource("account");
        tx.setNetAmount(initialDeposit);
        transactionRepository.save(tx);

        logger.info("Flexible plan created: {}, deducted from account balance: {}", plan.getId(), initialDeposit);
        recordDailyBalance(plan, LocalDate.now(), initialDeposit, BigDecimal.ZERO);

        String formattedDeposit = CurrencyFormatter.formatCurrency(initialDeposit);
        emailService.sendHtmlEmail(
                user.getEmail(),
                "New Flexible Savings Plan Created",
                "<p>Dear " + user.getFullName() + ",</p>" +
                        "<p>A new savings plan '" + plan.getName() + "' has been created successfully.</p>" +
                        "<p>Initial Deposit: " + formattedDeposit + "</p>" +
                        "<p>Maturity Date: " + (request.getMaturedAt() != null ? request.getMaturedAt().toString() : "N/A") + "</p>"
        );

        return new SavingsPlanResponse(
                plan.getId(),
                plan.getStatus().name(),
                CurrencyFormatter.formatCurrency(plan.getPrincipalBalance()),
                plan.getName(),
                plan.getMaturedAt(),
                plan.getCreatedAt(),
                SavingsType.FLEXIBLE,
                CurrencyFormatter.formatCurrency(plan.getAccruedInterest())
        );
    }

    @Transactional
    public TopUpResponse topUpFlexiblePlan(User user, TopUpRequest request) {
        if (!user.getPin().equals(request.getPin())) {
            throw new IllegalArgumentException("Invalid PIN");
        }

        Long planId = Long.valueOf(request.getPlanId());
        FlexiblePlan plan = getActivePlan(user, planId);
        BigDecimal topUpAmount = new BigDecimal(request.getAmount());

        if (topUpAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-up amount must be greater than zero");
        }
        Account account = accountService.getAccountByUserId(user.getId());
        if (account.getBalance().compareTo(topUpAmount) < 0) {
            throw new SavingsException("INSUFFICIENT_BALANCE", "Insufficient account balance");
        }

        account.setBalance(account.getBalance().subtract(topUpAmount));
        accountRepository.save(account);

        plan.setPrincipalBalance(plan.getPrincipalBalance().add(topUpAmount));
        planRepository.save(plan);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(topUpAmount);
        accountTransactionRepository.save(transaction);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlanType(SavingsType.FLEXIBLE);
        tx.setPlanId(plan.getId());
        tx.setType(TransactionType.TOPUP);
        tx.setAmount(topUpAmount);
        tx.setSource(request.getSource());
        tx.setNetAmount(topUpAmount);
        transactionRepository.save(tx);

        logger.info("Top-up successful for plan: {}", plan.getId());
        recordDailyBalance(plan, LocalDate.now(), topUpAmount, BigDecimal.ZERO);

        String formattedAmount = CurrencyFormatter.formatCurrency(topUpAmount);
        String formattedBalance = CurrencyFormatter.formatCurrency(plan.getPrincipalBalance());

        emailService.sendHtmlEmail(
                user.getEmail(),
                "Top-Up Successful",
                "<p>Dear " + user.getFullName() + ",</p>" +
                        "<p>You have successfully deposited " + formattedAmount + " into your savings plan '" + plan.getName() + "'.</p>" +
                        "<p>New Balance: " + formattedBalance + "</p>"
        );
        return new TopUpResponse(
                CurrencyFormatter.formatCurrency(plan.getPrincipalBalance()),
                "Top-up successful"
        );
    }

    @Transactional
    public WithdrawResponse withdrawFlexiblePlan(User user, WithdrawRequest request) {
        FlexiblePlan plan = getActivePlan(user, request.getPlanId());
        String currentMonth = YearMonth.now().toString();
        MonthlyActivity activity = monthlyActivityRepository.findByPlanIdAndMonth(plan.getId(), currentMonth)
                .orElse(new MonthlyActivity());

        if (activity.getId() == null) {
            activity.setUser(user);
            activity.setPlanId(plan.getId());
            activity.setPlanType(SavingsType.FLEXIBLE);
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
            throw new SavingsException("INSUFFICIENT_PLAN_BALANCE", "Insufficient balance in savings plan");
        }
        plan.setPrincipalBalance(plan.getPrincipalBalance().subtract(amount));
        planRepository.save(plan);

        Account account = accountService.getAccountByUserId(user.getId());
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        accountTransactionRepository.save(transaction);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlanType(SavingsType.FLEXIBLE);
        tx.setPlanId(plan.getId());
        tx.setType(TransactionType.WITHDRAWAL);
        tx.setAmount(amount);
        tx.setSource("wallet");
        tx.setNetAmount(amount);
        transactionRepository.save(tx);

        recordDailyBalance(plan, LocalDate.now(), BigDecimal.ZERO, amount);
        logger.info("Withdrawal from plan: {}, count: {}", plan.getId(), newCount);

        String formattedAmount = CurrencyFormatter.formatCurrency(amount);
        String formattedBalance = CurrencyFormatter.formatCurrency(plan.getPrincipalBalance());

        emailService.sendHtmlEmail(
                user.getEmail(),
                "Savings Plan Withdrawal",
                "<p>Dear " + user.getFullName() + ",</p>" +
                        "<p>You have successfully withdrawn " + formattedAmount + " from your savings plan '" + plan.getName() + "'.</p>" +
                        "<p>New Balance: " + formattedBalance + "</p>"
        );
        return new WithdrawResponse(
                CurrencyFormatter.formatCurrency(plan.getPrincipalBalance()),
                newCount,
                activity.isInterestForfeited(),
                "Withdrawal successful" + (activity.isInterestForfeited() ? ". Monthly interest will be forfeited." : "")
        );
    }

    @Transactional
    public LiquidateResponse liquidateFlexiblePlan(User user, LiquidateRequest request) {
        FlexiblePlan plan = getActivePlan(user, request.getPlanId());
        String currentMonth = YearMonth.now().toString();
        MonthlyActivity activity = monthlyActivityRepository.findByPlanIdAndMonth(plan.getId(), currentMonth)
                .orElse(new MonthlyActivity());

        BigDecimal payout = plan.getPrincipalBalance();
        if (payout.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SavingsException("INSUFFICIENT_PLAN_BALANCE", "No balance available to liquidate");
        }

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlanType(SavingsType.FLEXIBLE);
        tx.setPlanId(plan.getId());
        tx.setType(TransactionType.LIQUIDATION);
        tx.setAmount(payout);
        tx.setSource("wallet");
        tx.setNetAmount(payout);
        transactionRepository.save(tx);

        plan.setStatus(PlanStatus.CLOSED);
        plan.setPrincipalBalance(BigDecimal.ZERO);
        planRepository.save(plan);

        Account account = accountService.getAccountByUserId(user.getId());
        account.setBalance(account.getBalance().add(payout));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(payout);
        accountTransactionRepository.save(transaction);

        logger.info("Plan liquidated: {}", plan.getId());

        String formattedPayout = CurrencyFormatter.formatCurrency(payout);

        emailService.sendHtmlEmail(
                user.getEmail(),
                "Savings Plan Liquidation",
                "<p>Dear " + user.getFullName() + ",</p>" +
                        "<p>Your savings plan '" + plan.getName() + "' has been successfully liquidated.</p>" +
                        "<p>Payout Amount: " + formattedPayout + "</p>"
        );
        return new LiquidateResponse(
                CurrencyFormatter.formatCurrency(plan.getPrincipalBalance()),
                CurrencyFormatter.formatCurrency(BigDecimal.ZERO),
                CurrencyFormatter.formatCurrency(BigDecimal.ZERO),
                CurrencyFormatter.formatCurrency(payout),
                plan.getStatus().name()
        );
    }

    private FlexiblePlan getActivePlan(User user, @NotNull Long planId) {
        FlexiblePlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        if (!plan.getUser().getId().equals(user.getId()) || plan.getStatus() != PlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Invalid or inactive plan");
        }
        return plan;
    }

    @Transactional
    public SavingsPlanResponse rolloverFlexiblePlan(User user, Long oldPlanId, RolloverRequest request) {
        FlexiblePlan oldPlan = getActivePlan(user, oldPlanId);

        if (oldPlan.getMaturedAt() == null || LocalDate.now().isBefore(oldPlan.getMaturedAt().toLocalDate())) {
            logger.error("Attempted to rollover plan {} before maturity date: {}", oldPlanId, oldPlan.getMaturedAt());
            throw new PrematureRolloverException("Plan cannot be rolled over before its maturity date.");
        }

        BigDecimal accruedInterest = calculateAccruedInterest(oldPlan);
        BigDecimal withdrawalTax = accruedInterest.multiply(new BigDecimal("0.10"));
        BigDecimal netInterest = accruedInterest.subtract(withdrawalTax);
        BigDecimal rolloverPrincipal = oldPlan.getPrincipalBalance().add(netInterest);
        String formattedRolloverPrincipal = CurrencyFormatter.formatCurrency(rolloverPrincipal);

        FlexiblePlan newPlan = new FlexiblePlan();
        newPlan.setUser(user);
        newPlan.setName(request.getNewPlanName());
        newPlan.setPrincipalBalance(rolloverPrincipal);
        newPlan.setInterestHandling(InterestHandling.valueOf(request.getNewInterestHandling().toUpperCase()));
        newPlan.setMaturedAt(LocalDateTime.from(request.getNewMaturedAt()));
        planRepository.save(newPlan);

        oldPlan.setStatus(PlanStatus.CLOSED);
        oldPlan.setPrincipalBalance(BigDecimal.ZERO);
        planRepository.save(oldPlan);

        SavingsTransaction oldTx = new SavingsTransaction();
        oldTx.setPlanType(SavingsType.FLEXIBLE);
        oldTx.setPlanId(oldPlan.getId());
        oldTx.setType(TransactionType.ROLLOVER_WITHDRAWAL);
        oldTx.setAmount(oldPlan.getPrincipalBalance().add(accruedInterest));
        oldTx.setNetAmount(oldPlan.getPrincipalBalance().add(netInterest));
        oldTx.setSource("rollover");
        transactionRepository.save(oldTx);

        SavingsTransaction newTx = new SavingsTransaction();
        newTx.setPlanType(SavingsType.FLEXIBLE);
        newTx.setPlanId(newPlan.getId());
        newTx.setType(TransactionType.ROLLOVER_DEPOSIT);
        newTx.setAmount(rolloverPrincipal);
        newTx.setNetAmount(rolloverPrincipal);
        newTx.setSource("rollover");
        transactionRepository.save(newTx);

        logger.info("Plan {} rolled over to new plan {}", oldPlanId, newPlan.getId());
        emailService.sendHtmlEmail(
                user.getEmail(),
                "Savings Plan Rollover Successful",
                "<p>Dear " + user.getFullName() + ",</p>" +
                        "<p>Your savings plan '" + oldPlan.getName() + "' has been successfully rolled over to a new plan '" + request.getNewPlanName() + "'.</p>" +
                        "<p>Rollover Amount: " + formattedRolloverPrincipal + "</p>" +
                        "<p>New Maturity Date: " + (request.getNewMaturedAt() != null ? request.getNewMaturedAt().toString() : "N/A") + "</p>"
        );
        return new SavingsPlanResponse(
                newPlan.getId(),
                newPlan.getStatus().name(),
                CurrencyFormatter.formatCurrency(newPlan.getPrincipalBalance()),
                newPlan.getName(),
                newPlan.getMaturedAt(),
                newPlan.getCreatedAt(),
                SavingsType.FLEXIBLE,
                CurrencyFormatter.formatCurrency(newPlan.getAccruedInterest())
        );
    }

    public List<SavingsPlanResponseLite> getAllActivePlansLite(User user) {
        List<FlexiblePlan> plans = planRepository.findByUserIdAndStatus(user.getId(), PlanStatus.ACTIVE);
        return plans.stream()
                .map(this::convertToSavingsPlanResponseLite)
                .collect(Collectors.toList());
    }

    public SavingsPlanResponseLite getPlanByIdLite(User user, Long id) {
        FlexiblePlan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new SavingsException("NOT_AUTHORIZED", "Unauthorized access to plan");
        }
        return convertToSavingsPlanResponseLite(plan);
    }

    public List<SavingsTransactionResponse> getTransactionsForPlan(User user, Long planId) {
        FlexiblePlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new SavingsException("PLAN_NOT_FOUND", "Plan not found"));

        if (!plan.getUser().getId().equals(user.getId())) {
            throw new SavingsException("NOT_AUTHORIZED", "Unauthorized access to plan transactions");
        }

        List<SavingsTransaction> transactions = transactionRepository.findByPlan(SavingsType.FLEXIBLE, planId);
        return transactions.stream()
                .map(this::convertToSavingsTransactionResponse)
                .collect(Collectors.toList());
    }

    public AccruedInterestResponse getAccruedInterest(User user, Long planId, AccruedInterestRequest request) {
        FlexiblePlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new SavingsException("PLAN_NOT_FOUND", "Plan not found"));

        if (!plan.getUser().getId().equals(user.getId())) {
            throw new SavingsException("NOT_AUTHORIZED", "Unauthorized access to plan transactions");
        }

        Instant startInstant = request.getStartDate().toInstant(ZoneOffset.UTC);
        Instant endInstant = request.getEndDate().toInstant(ZoneOffset.UTC);

        List<SavingsTransaction> interestTransactions = transactionRepository.findByPlanIdAndTypeAndDateRange(
                SavingsType.FLEXIBLE, planId, TransactionType.INTEREST, startInstant, endInstant
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

    private void recordDailyBalance(FlexiblePlan plan, LocalDate date, BigDecimal deposit, BigDecimal withdrawal) {
        DailyBalance previousBalance = dailyBalanceRepository.findTopByPlanIdAndPlanTypeAndDateLessThanOrderByDateDesc(
                        plan.getId(), SavingsType.FLEXIBLE, date)
                .orElse(new DailyBalance(plan.getId(), SavingsType.FLEXIBLE, date.minusDays(1), BigDecimal.ZERO, BigDecimal.ZERO));

        BigDecimal currentBalance = previousBalance.getClosingBalance();
        BigDecimal netInflow = deposit.subtract(withdrawal);
        BigDecimal newClosingBalance = currentBalance.add(netInflow);

        DailyBalance todayBalance = new DailyBalance();
        todayBalance.setPlanId(plan.getId());
        todayBalance.setPlanType(SavingsType.FLEXIBLE);
        todayBalance.setDate(date);
        todayBalance.setNetBalance(netInflow);
        todayBalance.setClosingBalance(newClosingBalance);
        dailyBalanceRepository.save(todayBalance);
    }

    private BigDecimal calculateAccruedInterest(FlexiblePlan plan) {
        List<SavingsTransaction> interestTransactions = transactionRepository.findByPlanAndType(SavingsType.FLEXIBLE, plan.getId(), TransactionType.INTEREST);
        return interestTransactions.stream()
                .map(SavingsTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SavingsPlanResponseLite convertToSavingsPlanResponseLite(FlexiblePlan plan) {
        SavingsPlanResponseLite response = new SavingsPlanResponseLite();
        response.setId(plan.getId());
        response.setName(plan.getName());
        response.setStatus(plan.getStatus().name());
        response.setPrincipalBalance(CurrencyFormatter.formatCurrency(plan.getPrincipalBalance()));
        response.setInterestHandling(plan.getInterestHandling().name());
        response.setCreatedAt(plan.getCreatedAt());
        response.setMaturedAt(plan.getMaturedAt());
        response.setType(SavingsType.FLEXIBLE.name());
        response.setAccruedInterest(CurrencyFormatter.formatCurrency(plan.getAccruedInterest()));
        return response;
    }

    private SavingsTransactionResponse convertToSavingsTransactionResponse(SavingsTransaction transaction) {
        SavingsTransactionResponse response = new SavingsTransactionResponse();
        response.setId(transaction.getId());
        response.setType(transaction.getType().name());
        response.setAmount(CurrencyFormatter.formatCurrency(transaction.getAmount()));
        response.setSource(transaction.getSource());
        response.setNetAmount(CurrencyFormatter.formatCurrency(transaction.getNetAmount()));
        response.setTimestamp(transaction.getTimestamp());

        FlexiblePlan plan = planRepository.findById(transaction.getPlanId())
                .orElseThrow(() -> new SavingsException("PLAN_NOT_FOUND", "Plan not found"));

        PlanInfo planInfo = new PlanInfo();
        planInfo.setId(plan.getId());
        planInfo.setName(plan.getName());
        planInfo.setStatus(plan.getStatus().name());
        planInfo.setPrincipalBalance(CurrencyFormatter.formatCurrency(plan.getPrincipalBalance()));
        planInfo.setInterestHandling(plan.getInterestHandling().name());
        planInfo.setCreatedAt(plan.getCreatedAt());
        planInfo.setMaturedAt(plan.getMaturedAt());
        planInfo.setType(SavingsType.FLEXIBLE.name());
        planInfo.setAccruedInterest(CurrencyFormatter.formatCurrency(plan.getAccruedInterest()));

        response.setPlan(planInfo);
        return response;
    }
}