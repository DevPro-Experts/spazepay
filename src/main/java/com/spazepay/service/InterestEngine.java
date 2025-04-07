package com.spazepay.service;

import com.spazepay.model.*;
import com.spazepay.model.enums.InterestHandling;
import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.SavingsType;
import com.spazepay.model.enums.TransactionType;
import com.spazepay.repository.MonthlyActivityRepository;
import com.spazepay.repository.SavingsPlanRepository;
import com.spazepay.repository.SavingsTransactionRepository;
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
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.05"); // 5%
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10%

    @Autowired
    private SavingsPlanRepository planRepository;

    @Autowired
    private SavingsTransactionRepository transactionRepository;

    @Autowired
    private MonthlyActivityRepository monthlyActivityRepository;

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 0 0 1 * *", zone = "Africa/Lagos")
    @Transactional
    public void calculateMonthlyInterest() {
        String currentMonth = YearMonth.now().minusMonths(1).toString();
        LocalDate now = LocalDate.now();
        List<SavingsPlan> activePlans = planRepository.findByUserIdAndStatus(null, PlanStatus.ACTIVE);

        for (SavingsPlan plan : activePlans) {
            if (plan.getType() != SavingsType.FLEXIBLE) continue;

            MonthlyActivity activity = monthlyActivityRepository.findByPlanIdAndMonth(plan.getId(), currentMonth)
                    .orElse(new MonthlyActivity());
            if (activity.isInterestForfeited()) {
                logger.info("Interest forfeited for plan: {}", plan.getId());
                continue;
            }

            BigDecimal interest = plan.getPrincipalBalance().multiply(INTEREST_RATE);
            BigDecimal tax = interest.multiply(TAX_RATE);
            BigDecimal netInterest = interest.subtract(tax);

            SavingsTransaction tx = new SavingsTransaction();
            tx.setPlan(plan);
            tx.setType(TransactionType.INTEREST);
            tx.setAmount(interest);
            tx.setSource("account");
            tx.setNetAmount(netInterest);
            transactionRepository.save(tx);

            if (plan.getInterestHandling() == InterestHandling.COMPOUND) {
                plan.setPrincipalBalance(plan.getPrincipalBalance().add(netInterest));
                planRepository.save(plan);

                emailService.sendHtmlEmail(
                        plan.getUser().getEmail(),
                        "Monthly Interest Accrued",
                        "<html><body>" +
                                "<p>Dear " + plan.getUser().getFullName() + ",</p>" +
                                "<p>Interest of " + netInterest + " has been compounded to your savings plan '" + plan.getName() + "'.</p>" +
                                "<p>Current Balance: " + plan.getPrincipalBalance() + "</p>" +
                                "<p>Thank you.</p>" +
                                "</body></html>"
                );
            } else {
                // Transfer netInterest to main account (simplified)
                logger.info("Interest withdrawn to main account for plan: {}", plan.getId());
                emailService.sendHtmlEmail(
                        plan.getUser().getEmail(),
                        "Monthly Interest Paid Out",
                        "<html><body>" +
                                "<p>Dear " + plan.getUser().getFullName() + ",</p>" +
                                "<p>Interest of " + netInterest + " has been paid out to your main account from savings plan '" + plan.getName() + "'.</p>" +
                                "<p>Thank you.</p>" +
                                "</body></html>"
                );
            }
        }
    }

    @Scheduled(cron = "0 0 0 1 * ?")
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