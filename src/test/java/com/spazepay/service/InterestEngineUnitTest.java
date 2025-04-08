package com.spazepay.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class InterestEngineUnitTest {

    @Autowired
    private InterestEngine interestEngine;

    @Test
    public void testCalculateDailyInterestRate() {
        // Given: Access the private method via reflection or make it package-private for testing
        BigDecimal expectedRate = new BigDecimal("0.05")
                .divide(new BigDecimal("365"), 10, RoundingMode.DOWN);
        BigDecimal actualRate = invokeCalculateDailyInterestRate();

        // Then: Verify the calculation
        assertEquals(expectedRate, actualRate, "Daily interest rate should be 0.05 / 365");
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