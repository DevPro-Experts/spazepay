package com.spazepay.repository;

import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.savings.TargetPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TargetPlanRepository extends JpaRepository<TargetPlan, Long> {
    List<TargetPlan> findByUserIdAndStatus(Long userId, PlanStatus status);
    List<TargetPlan> findByStatus(PlanStatus status);
}