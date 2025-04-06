package com.spazepay.repository;

import com.spazepay.model.SavingsPlan;
import com.spazepay.model.enums.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsPlanRepository extends JpaRepository<SavingsPlan, Long> {
    List<SavingsPlan> findByUserIdAndStatus(Long userId, PlanStatus status);
    long countByUserIdAndStatus(Long userId, PlanStatus status);
}