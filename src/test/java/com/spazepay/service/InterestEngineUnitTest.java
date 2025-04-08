package com.spazepay.service;

import com.spazepay.model.SavingsPlan;
import com.spazepay.repository.SavingsPlanRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
public class InterestEngineUnitTest {

    @Autowired
    private InterestEngine interestEngine;

    @Mock
    private SavingsPlanRepository planRepository;

    @Test
    void testCalculateDailyInterestRate() {
        when(planRepository.findById(anyLong())).thenReturn(Optional.of(new SavingsPlan()));
        BigDecimal rate = interestEngine.calculateDailyInterestRate(); // Adjust method name if needed
        assertNotNull(rate, "Daily interest rate should not be null");
    }

    // Helper method to access private method (alternatively, make it package-private)
    private BigDecimal invokeCalculateDailyInterestRate() {
        try {
            java.lang.reflect.Method method = InterestEngine.class.getDeclaredMethod("calculateDailyInterestRate");
            method.setAccessible(true);
            return (BigDecimal) method.invoke(interestEngine);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke calculateDailyInterestRate", e);
        }
    }
}