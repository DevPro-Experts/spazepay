package com.spazepay.service;

import com.spazepay.model.*;
import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.SavingsType;
import com.spazepay.model.enums.TransactionType;
import com.spazepay.repository.DailyBalanceRepository;
import com.spazepay.repository.MonthlyActivityRepository;
import com.spazepay.repository.SavingsPlanRepository;
import com.spazepay.repository.SavingsTransactionRepository;
import com.spazepay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"spring.mail.host="})
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("deprecation")
class InterestEngineTest {

    // Unit test setup
    private InterestEngine interestEngineForUnitTest;
    private SavingsPlanRepository planRepositoryMock;
    private DailyBalanceRepository dailyBalanceRepositoryMock;
    private SavingsTransactionRepository transactionRepositoryMock;
    private MonthlyActivityRepository monthlyActivityRepositoryMock;
    private EmailService emailServiceMock;

    // Integration test setup
    @Autowired
    private InterestEngine interestEngine;

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

    @BeforeEach
    void setUp() {
        // Unit test mocks
        planRepositoryMock = mock(SavingsPlanRepository.class);
        dailyBalanceRepositoryMock = mock(DailyBalanceRepository.class);
        transactionRepositoryMock = mock(SavingsTransactionRepository.class);
        monthlyActivityRepositoryMock = mock(MonthlyActivityRepository.class);
        emailServiceMock = mock(EmailService.class);
        interestEngineForUnitTest = new InterestEngine(
                planRepositoryMock, dailyBalanceRepositoryMock, transactionRepositoryMock,
                monthlyActivityRepositoryMock, emailServiceMock);
    }

    // Unit Tests

    @Test
    void testCalculateDailyInterestRate() {
        BigDecimal expectedRate = new BigDecimal("0.05")
                .divide(new BigDecimal("365"), 10, BigDecimal.ROUND_DOWN);
        BigDecimal actualRate = interestEngineForUnitTest.calculateDailyInterestRate();
        assertEquals(expectedRate, actualRate, "Daily interest rate should match expected value");
    }

    // Integration Tests

    @Test
    void testApplyDailyInterest() {
        // Arrange
        SavingsPlan plan = createTestSavingsPlan();
        planRepository.save(plan);

        DailyBalance dailyBalance = new DailyBalance();
        dailyBalance.setPlan(plan);
        dailyBalance.setDate(LocalDate.now().minusDays(2));
        dailyBalance.setNetBalance(BigDecimal.valueOf(1000));
        dailyBalanceRepository.save(dailyBalance);

        // Act
        interestEngine.applyDailyInterest();

        // Assert
        SavingsPlan updatedPlan = planRepository.findById(plan.getId()).orElseThrow();
        BigDecimal expectedGrossInterest = calculateExpectedGrossInterest(BigDecimal.valueOf(1000));
        BigDecimal expectedNetInterest = calculateExpectedNetInterest(BigDecimal.valueOf(1000));
        assertEquals(
                BigDecimal.valueOf(1000).add(expectedNetInterest).setScale(2, BigDecimal.ROUND_HALF_EVEN),
                updatedPlan.getPrincipalBalance(),
                "Principal should increase by net interest"
        );
        assertEquals(
                expectedGrossInterest.setScale(2, BigDecimal.ROUND_HALF_EVEN),
                updatedPlan.getAccruedInterest(),
                "Accrued interest should reflect gross interest"
        );

        List<SavingsTransaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size(), "One transaction should be recorded");
        assertEquals(TransactionType.INTEREST, transactions.get(0).getType());
        assertEquals(expectedNetInterest, transactions.get(0).getNetAmount());

        verify(emailService, times(1)).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void testApplyDailyInterestWithForfeiture() {
        // Arrange
        SavingsPlan plan = createTestSavingsPlan();
        planRepository.save(plan);

        DailyBalance dailyBalance = new DailyBalance();
        dailyBalance.setPlan(plan);
        dailyBalance.setDate(LocalDate.now().minusDays(2));
        dailyBalance.setNetBalance(BigDecimal.valueOf(1000));
        dailyBalanceRepository.save(dailyBalance);

        MonthlyActivity activity = new MonthlyActivity();
        activity.setPlan(plan);
        activity.setUser(plan.getUser());
        activity.setMonth(YearMonth.now().toString());
        activity.setInterestForfeited(true);
        monthlyActivityRepository.save(activity);

        // Act
        interestEngine.applyDailyInterest();

        // Assert
        SavingsPlan updatedPlan = planRepository.findById(plan.getId()).orElseThrow();
        assertEquals(BigDecimal.valueOf(1000), updatedPlan.getPrincipalBalance(), "Principal should not increase");
        assertEquals(BigDecimal.ZERO, updatedPlan.getAccruedInterest(), "Accrued interest should remain zero");

        List<SavingsTransaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.isEmpty(), "No transactions should be recorded");

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void testApplyDailyInterestNoBalance() {
        // Arrange
        SavingsPlan plan = createTestSavingsPlan();
        planRepository.save(plan);

        // No daily balance record

        // Act
        interestEngine.applyDailyInterest();

        // Assert
        SavingsPlan updatedPlan = planRepository.findById(plan.getId()).orElseThrow();
        assertEquals(BigDecimal.valueOf(1000), updatedPlan.getPrincipalBalance(), "Principal should not change");
        assertEquals(BigDecimal.ZERO, updatedPlan.getAccruedInterest(), "Accrued interest should remain zero");

        List<SavingsTransaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.isEmpty(), "No transactions should be recorded");
    }

    @Test
    void testSendMonthlyInterestSummary() {
        // Arrange
        SavingsPlan plan = createTestSavingsPlan();
        planRepository.save(plan);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setPlan(plan);
        tx.setType(TransactionType.INTEREST);
        tx.setAmount(BigDecimal.valueOf(0.05));
        tx.setNetAmount(BigDecimal.valueOf(0.04));
        tx.setSource("system");
        tx.setTimestamp(Instant.now().minus(30, ChronoUnit.DAYS)); // Set to previous month
        transactionRepository.save(tx);

        // Act
        interestEngine.sendMonthlyInterestSummary();

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendHtmlEmail(any(), any(), emailCaptor.capture());
        String emailContent = emailCaptor.getValue();
        assertTrue(emailContent.contains("0.04"), "Email should contain net interest amount");
    }

    @Test
    void testResetMonthlyCounters() {
        // Arrange
        SavingsPlan plan = createTestSavingsPlan();
        planRepository.save(plan);

        MonthlyActivity activity = new MonthlyActivity();
        activity.setPlan(plan);
        activity.setUser(plan.getUser());
        activity.setMonth(YearMonth.now().minusMonths(1).toString());
        activity.setWithdrawalCount(3);
        activity.setInterestForfeited(true);
        monthlyActivityRepository.save(activity);

        // Act
        interestEngine.resetMonthlyCounters();

        // Assert
        MonthlyActivity updatedActivity = monthlyActivityRepository.findByPlanIdAndMonth(plan.getId(), activity.getMonth())
                .orElseThrow();
        assertEquals(0, updatedActivity.getWithdrawalCount(), "Withdrawal count should reset");
        assertFalse(updatedActivity.isInterestForfeited(), "Interest forfeiture should reset");
    }

    // Helper Methods

    private SavingsPlan createTestSavingsPlan() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPin("1234");
        userRepository.save(user);

        SavingsPlan plan = new SavingsPlan();
        plan.setName("Test Plan");
        plan.setType(SavingsType.FLEXIBLE);
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setPrincipalBalance(BigDecimal.valueOf(1000));
        plan.setAccruedInterest(BigDecimal.ZERO);
        plan.setUser(user);
        return plan;
    }

    private BigDecimal calculateExpectedNetInterest(BigDecimal principal) {
        BigDecimal dailyRate = new BigDecimal("0.05").divide(new BigDecimal("365"), 10, BigDecimal.ROUND_DOWN);
        BigDecimal grossInterest = principal.multiply(dailyRate).setScale(2, BigDecimal.ROUND_HALF_EVEN);
        BigDecimal tax = grossInterest.multiply(new BigDecimal("0.10")).setScale(2, BigDecimal.ROUND_HALF_EVEN);
        return grossInterest.subtract(tax);
    }

    private BigDecimal calculateExpectedGrossInterest(BigDecimal principal) {
        BigDecimal dailyRate = new BigDecimal("0.05").divide(new BigDecimal("365"), 10, BigDecimal.ROUND_DOWN);
        return principal.multiply(dailyRate).setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }
}