package com.spazepay.repository;

import com.spazepay.model.SavingsTransaction;
import com.spazepay.model.enums.SavingsType;
import com.spazepay.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {
    @Query("SELECT t FROM SavingsTransaction t WHERE t.planType = :planType AND t.planId = :planId")
    List<SavingsTransaction> findByPlan(@Param("planType") SavingsType planType, @Param("planId") Long planId);

    @Query("SELECT t FROM SavingsTransaction t WHERE t.planType = :planType AND t.planId = :planId AND t.type = :type")
    List<SavingsTransaction> findByPlanAndType(
            @Param("planType") SavingsType planType,
            @Param("planId") Long planId,
            @Param("type") TransactionType type);

    @Query("SELECT t FROM SavingsTransaction t WHERE t.planType = :planType AND t.planId = :planId AND t.type = :type AND TO_CHAR(t.timestamp, 'YYYY-MM') = :month")
    List<SavingsTransaction> findByPlanAndTypeAndMonth(
            @Param("planType") SavingsType planType,
            @Param("planId") Long planId,
            @Param("type") TransactionType type,
            @Param("month") String month);

    @Query("SELECT t FROM SavingsTransaction t WHERE t.planType = :planType AND t.planId = :planId AND t.type = :type AND t.timestamp BETWEEN :startDate AND :endDate")
    List<SavingsTransaction> findByPlanIdAndTypeAndDateRange(
            @Param("planType") SavingsType planType,
            @Param("planId") Long planId,
            @Param("type") TransactionType type,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT t FROM SavingsTransaction t WHERE t.planId = :planId AND t.planType = :planType AND t.type = :type " +
            "AND FUNCTION('MONTH', t.timestamp) = :month AND FUNCTION('YEAR', t.timestamp) = :year")
    List<SavingsTransaction> findByPlanIdAndPlanTypeAndTypeAndMonth(
            @Param("planId") Long planId,
            @Param("planType") SavingsType planType,
            @Param("type") TransactionType type,
            @Param("month") int month,
            @Param("year") int year);
}