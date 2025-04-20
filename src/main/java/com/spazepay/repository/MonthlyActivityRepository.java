package com.spazepay.repository;

import com.spazepay.model.MonthlyActivity;
import com.spazepay.model.enums.SavingsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MonthlyActivityRepository extends JpaRepository<MonthlyActivity, Long> {
    @Query("SELECT m FROM MonthlyActivity m WHERE m.planId = :planId AND m.month = :month")
    Optional<MonthlyActivity> findByPlanIdAndMonth(@Param("planId") Long planId, @Param("month") String month);

    Optional<MonthlyActivity> findByPlanIdAndPlanTypeAndMonth(Long planId, SavingsType planType, String month);

}