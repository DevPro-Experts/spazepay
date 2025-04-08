package com.spazepay.repository;

import com.spazepay.model.SavingsPlan;
import com.spazepay.model.SavingsTransaction;
import com.spazepay.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {
    List<SavingsTransaction> findByPlanIdAndTypeAndTimestampBetween(Long planId, TransactionType type, Instant start, Instant end);

    List<SavingsTransaction> findByPlan(SavingsPlan plan);

    List<SavingsTransaction> findByPlanAndType(SavingsPlan plan, TransactionType transactionType);

    @Query("SELECT t FROM SavingsTransaction t " +
            "WHERE t.plan = :plan " +
            "AND t.type = :type " +
            "AND TO_CHAR(t.timestamp, 'YYYY-MM') = :month")
    List<SavingsTransaction> findByPlanAndTypeAndMonth(
            @Param("plan") SavingsPlan plan,
            @Param("type") TransactionType type,
            @Param("month") String month);
}