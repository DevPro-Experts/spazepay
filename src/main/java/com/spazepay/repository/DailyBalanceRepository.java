package com.spazepay.repository;

import com.spazepay.model.DailyBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyBalanceRepository extends JpaRepository<DailyBalance, Long> {

    List<DailyBalance> findByPlanIdAndDate(Long planId, LocalDate date);

    // Find the most recent daily balance record before a given date for a specific plan
    Optional<DailyBalance> findTopByPlanIdAndDateLessThanOrderByDateDesc(Long planId, LocalDate date);

    // The query to retrieve daily balances for a plan within a date range
    // List<DailyBalance> findByPlanIdAndDateBetween(Long planId, LocalDate startDate, LocalDate endDate);
}