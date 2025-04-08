package com.spazepay.service;

import com.spazepay.model.*;
import com.spazepay.model.enums.*;
import com.spazepay.repository.*;
import com.spazepay.util.CurrencyFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InterestEngineIntegrationTest {

    @Autowired
    private SavingsPlanRepository planRepository;

    @Autowired
    private DailyBalanceRepository dailyBalanceRepository;

    @Autowired
    private SavingsTransactionRepository transactionRepository;

    @Autowired
    private MonthlyActivityRepository monthlyActivityRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private EmailService emailService;

    @Autowired
    private InterestEngine interestEngine;

    private SavingsPlan plan;
    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPin("1234");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setGender("Male");
        user.setAddress("123 Test Street");
        user.setNationality("Nigerian");
        user.setPhoneNumber("08012345678");
        user.setPassword("testpassword");
        user.setBvnOrNin("12345678901");
        user.setPassportPhoto("test-photo.jpg");
        user = userRepository.save(user);

        plan = new SavingsPlan();
        plan.setUser(user);
        plan.setName("Test Plan");
        plan.setPrincipalBalance(new BigDecimal("1000.00"));
        plan.setType(SavingsType.FLEXIBLE);
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setInterestHandling(InterestHandling.COMPOUND);
        plan = planRepository.save(plan);

        DailyBalance balance = new DailyBalance();
        balance.setPlan(plan);
        balance.setDate(LocalDate.now().minusDays(2));
        balance.setNetBalance(new BigDecimal("1000.00"));
        balance.setClosingBalance(new BigDecimal("1000.00"));
        dailyBalanceRepository.save(balance);
    }

    // Existing test methods remain unchanged
    @Test
    public void testApplyDailyInterest() {
        MonthlyActivity activity = new MonthlyActivity();
        activity.setPlan(plan);
        activity.setUser(user);
        activity.setMonth(YearMonth.now().toString());
        activity.setWithdrawalCount(0);
        activity.setInterestForfeited(false);
        monthlyActivityRepository.save(activity);

        interestEngine.applyDailyInterest();

        SavingsPlan updatedPlan = planRepository.findById(plan.getId()).get();
        List<SavingsTransaction> transactions = transactionRepository.findByPlan(plan);

        BigDecimal dailyRate = new BigDecimal("0.05").divide(new BigDecimal("365"), 10, RoundingMode.DOWN);
        BigDecimal grossInterest = new BigDecimal("1000.00").multiply(dailyRate).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal tax = grossInterest.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal netInterest = grossInterest.subtract(tax).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal expectedBalance = new BigDecimal("1000.00").add(netInterest);

        assertEquals(expectedBalance.setScale(2, RoundingMode.HALF_EVEN), updatedPlan.getPrincipalBalance(),
                "Principal balance should increase by net interest");
        assertEquals(1, transactions.size(), "One interest transaction should be recorded");
        assertEquals(grossInterest, transactions.get(0).getAmount(), "Transaction amount should be gross interest");
        assertEquals(netInterest, transactions.get(0).getNetAmount(), "Transaction net amount should be after tax");

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendHtmlEmail(eq(user.getEmail()), eq("Daily Interest Accrued"), emailCaptor.capture());
        String emailContent = emailCaptor.getValue();
        assertTrue(emailContent.contains(CurrencyFormatter.formatCurrency(netInterest)), "Email should contain net interest");
    }

    @Test
    public void testApplyDailyInterestWithForfeiture() {
        MonthlyActivity activity = new MonthlyActivity();
        activity.setPlan(plan);
        activity.setUser(user);
        activity.setMonth(YearMonth.now().toString());
        activity.setWithdrawalCount(5);
        activity.setInterestForfeited(true);
        monthlyActivityRepository.save(activity);

        interestEngine.applyDailyInterest();

        SavingsPlan updatedPlan = planRepository.findById(plan.getId()).get();
        List<SavingsTransaction> transactions = transactionRepository.findByPlan(plan);

        assertEquals(new BigDecimal("1000.00"), updatedPlan.getPrincipalBalance(),
                "Principal balance should not change due to forfeiture");
        assertEquals(0, transactions.size(), "No transactions should be recorded");
        verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendMonthlyInterestSummary() {
        String previousMonth = YearMonth.now().minusMonths(1).toString();
        SavingsTransaction tx1 = new SavingsTransaction();
        tx1.setPlan(plan);
        tx1.setType(TransactionType.INTEREST);
        tx1.setAmount(new BigDecimal("0.14"));
        tx1.setNetAmount(new BigDecimal("0.13"));
        tx1.setSource("system");
        tx1.setTimestamp(Instant.parse(previousMonth + "-15T00:00:00Z"));
        transactionRepository.save(tx1);

        SavingsTransaction tx2 = new SavingsTransaction();
        tx2.setPlan(plan);
        tx2.setType(TransactionType.INTEREST);
        tx2.setAmount(new BigDecimal("0.14"));
        tx2.setNetAmount(new BigDecimal("0.13"));
        tx2.setSource("system");
        tx2.setTimestamp(Instant.now().minus(30, ChronoUnit.DAYS));
        transactionRepository.save(tx2);

        interestEngine.sendMonthlyInterestSummary();

        BigDecimal expectedTotal = new BigDecimal("0.26");
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendHtmlEmail(eq("test@example.com"), eq("Your Monthly Interest Summary"), emailCaptor.capture());
        String emailContent = emailCaptor.getValue();
        assertTrue(emailContent.contains(CurrencyFormatter.formatCurrency(expectedTotal)));
    }
}