package com.spazepay.repository;

import com.spazepay.model.MonthlyActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyActivityRepository extends JpaRepository<MonthlyActivity, Long> {
    Optional<MonthlyActivity> findByPlanIdAndMonth(Long planId, String month);
}