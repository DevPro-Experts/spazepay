package com.spazepay.repository;

import com.spazepay.model.SavingsPlan;
import com.spazepay.model.SavingsTransaction;
import com.spazepay.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {
    List<SavingsTransaction> findByPlanIdAndTypeAndTimestampBetween(Long planId, TransactionType type, Instant start, Instant end);

    List<SavingsTransaction> findByPlan(SavingsPlan plan);
}