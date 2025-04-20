package com.spazepay.repository;

import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.savings.FixedPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixedPlanRepository extends JpaRepository<FixedPlan, Long> {
    List<FixedPlan> findByUserIdAndStatus(Long userId, PlanStatus status);
    List<FixedPlan> findByStatus(PlanStatus status);
}