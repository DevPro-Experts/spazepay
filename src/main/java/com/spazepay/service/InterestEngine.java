package com.spazepay.service;

import com.spazepay.model.*;
import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.SavingsType;
import com.spazepay.model.enums.TransactionType;
import com.spazepay.repository.DailyBalanceRepository;
import com.spazepay.repository.MonthlyActivityRepository;
import com.spazepay.repository.SavingsPlanRepository;
import com.spazepay.repository.SavingsTransactionRepository;
import com.spazepay.util.CurrencyFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class InterestEngine {

    private static final Logger logger = LoggerFactory.getLogger(InterestEngine.class);
    private static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.05"); // 5% annual
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10% tax
    private static final int DAYS_IN_YEAR = 365;

    @Autowired
    private SavingsPlanRepository planRepository;

    @Autowired
    private SavingsTransactionRepository transactionRepository;

    @Autowired
    private MonthlyActivityRepository monthlyActivityRepository;

    @Autowired
    private DailyBalanceRepository dailyBalanceRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    public InterestEngine(
            SavingsPlanRepository planRepository,
            DailyBalanceRepository dailyBalanceRepository,
            SavingsTransactionRepository transactionRepository,
            MonthlyActivityRepository monthlyActivityRepository,
            EmailService emailService) {
        this.planRepository = planRepository;
        this.dailyBalanceRepository = dailyBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.monthlyActivityRepository = monthlyActivityRepository;
        this.emailService = emailService;
    }

    BigDecimal calculateDailyInterestRate() {
        return ANNUAL_INTEREST_RATE.divide(new BigDecimal(DAYS_IN_YEAR), 10, BigDecimal.ROUND_DOWN);
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Africa/Lagos")
    @Transactional
    public void applyDailyInterest() {
        LocalDate today = LocalDate.now();
        LocalDate interestApplicableDate = today.minusDays(2); // Interest applies to net inflow 2 days ago
        String currentMonth = YearMonth.now().toString();

        List<SavingsPlan> activePlans = planRepository.findByStatus(PlanStatus.ACTIVE);

        for (SavingsPlan plan : activePlans) {
            if (plan.getType() != SavingsType.FLEXIBLE) continue;

            // Check forfeiture status for the current month
            MonthlyActivity activity = monthlyActivityRepository.findByPlanIdAndMonth(plan.getId(), currentMonth)
                    .orElse(new MonthlyActivity());
            if (activity.isInterestForfeited()) {
                logger.info("Daily interest forfeited for plan: {} due to monthly activity", plan.getId());
                continue;
            }

            // Fetch all daily balances and pick the first (or handle duplicates)
            List<DailyBalance> dailyBalances = dailyBalanceRepository.findByPlanIdAndDate(plan.getId(), interestApplicableDate);
            DailyBalance dailyBalance = dailyBalances.isEmpty() ? null : dailyBalances.get(0);

            if (dailyBalance != null && dailyBalance.getNetBalance().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal principalForInterest = dailyBalance.getNetBalance();
                BigDecimal dailyInterestRate = calculateDailyInterestRate();
                BigDecimal grossInterest = principalForInterest.multiply(dailyInterestRate).setScale(2, BigDecimal.ROUND_HALF_EVEN);
                BigDecimal tax = grossInterest.multiply(TAX_RATE).setScale(2, BigDecimal.ROUND_HALF_EVEN);
                BigDecimal netInterest = grossInterest.subtract(tax).setScale(2, BigDecimal.ROUND_HALF_EVEN);

                // Update accrued_interest (initialize if null)
                BigDecimal currentAccruedInterest = plan.getAccruedInterest() != null ? plan.getAccruedInterest() : BigDecimal.ZERO;
                plan.setAccruedInterest(currentAccruedInterest.add(grossInterest));

                // Compound the net interest into the principal balance
                plan.setPrincipalBalance(plan.getPrincipalBalance().add(netInterest));
                planRepository.save(plan);

                // Record the interest transaction
                SavingsTransaction tx = new SavingsTransaction();
                tx.setPlan(plan);
                tx.setType(TransactionType.INTEREST);
                tx.setAmount(grossInterest);
                tx.setSource("system");
                tx.setNetAmount(netInterest);
                transactionRepository.save(tx);

                String formattedGrossInterest = CurrencyFormatter.formatCurrency(grossInterest);
                String formattedNetInterest = CurrencyFormatter.formatCurrency(netInterest);
                String formattedBalance = CurrencyFormatter.formatCurrency(plan.getPrincipalBalance());

                emailService.sendHtmlEmail(
                        plan.getUser().getEmail(),
                        "Daily Interest Accrued",
                        "<html><body>" +
                                "<p>Dear " + plan.getUser().getFullName() + ",</p>" +
                                "<p>Daily interest of " + formattedGrossInterest + " (net: " + formattedNetInterest + " after tax) " +
                                "has been compounded to your savings plan '" + plan.getName() + "'.</p>" +
                                "<p>Current Balance: " + formattedBalance + "</p>" +
                                "<p>Thank you.</p>" +
                                "</body></html>"
                );

                logger.info("Daily interest of {} (net: {}) applied to plan {} for date {}",
                        grossInterest, netInterest, plan.getId(), interestApplicableDate);
            }
        }
    }

    @Scheduled(cron = "0 0 0 1 * *", zone = "Africa/Lagos") // Runs monthly on the 1st at midnight
    @Transactional
    public void sendMonthlyInterestSummary() {
        String previousMonth = YearMonth.now().minusMonths(1).toString();
        List<SavingsPlan> activePlans = planRepository.findByStatus(PlanStatus.ACTIVE);

        for (SavingsPlan plan : activePlans) {
            if (plan.getType() != SavingsType.FLEXIBLE) continue;

            // Calculate total accrued interest for the previous month
            List<SavingsTransaction> interestTransactions = transactionRepository
                    .findByPlanAndTypeAndMonth(plan, TransactionType.INTEREST, previousMonth);
            BigDecimal totalNetInterest = interestTransactions.stream()
                    .map(SavingsTransaction::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalNetInterest.compareTo(BigDecimal.ZERO) > 0) {
                String formattedTotalInterest = CurrencyFormatter.formatCurrency(totalNetInterest);
                String formattedBalance = CurrencyFormatter.formatCurrency(plan.getPrincipalBalance());

                emailService.sendHtmlEmail(
                        plan.getUser().getEmail(),
                        "Your Monthly Interest Summary",
                        "<html><body>" +
                                "<p>Dear " + plan.getUser().getFullName() + ",</p>" +
                                "<p>Congratulations! Last month, your savings plan '" + plan.getName() + "' earned a total of " +
                                formattedTotalInterest + " in interest.</p>" +
                                "<p>Current Balance: " + formattedBalance + "</p>" +
                                "<p>Keep saving with us to watch your money grow!</p>" +
                                "<p>Thank you.</p>" +
                                "</body></html>"
                );

                logger.info("Monthly interest summary of {} sent for plan {}", totalNetInterest, plan.getId());
            }
        }
    }

    @Scheduled(cron = "0 0 0 1 * ?", zone = "Africa/Lagos") // Reset counters monthly
    public void resetMonthlyCounters() {
        String currentMonth = YearMonth.now().toString();
        List<MonthlyActivity> activities = monthlyActivityRepository.findAll();
        for (MonthlyActivity activity : activities) {
            if (!activity.getMonth().equals(currentMonth)) {
                activity.setWithdrawalCount(0);
                activity.setInterestForfeited(false);
                monthlyActivityRepository.save(activity);
            }
        }
    }
}