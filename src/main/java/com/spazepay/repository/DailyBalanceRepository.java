package com.spazepay.repository;

import com.spazepay.model.DailyBalance;
import com.spazepay.model.enums.SavingsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyBalanceRepository extends JpaRepository<DailyBalance, Long> {
    @Query("SELECT d FROM DailyBalance d WHERE d.planId = :planId AND d.planType = :planType AND d.date < :date ORDER BY d.date DESC LIMIT 1")
    Optional<DailyBalance> findTopByPlanIdAndPlanTypeAndDateLessThanOrderByDateDesc(
            @Param("planId") Long planId,
            @Param("planType") SavingsType planType,
            @Param("date") LocalDate date);

    List<DailyBalance> findByPlanIdAndPlanTypeAndDate(Long planId, SavingsType planType, LocalDate date);
}