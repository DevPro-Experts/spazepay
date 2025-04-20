package com.spazepay.repository;

import com.spazepay.model.savings.FlexiblePlan;
import com.spazepay.model.enums.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlexiblePlanRepository extends JpaRepository<FlexiblePlan, Long> {
    List<FlexiblePlan> findByUserIdAndStatus(Long userId, PlanStatus status);
    long countByUserIdAndStatus(Long userId, PlanStatus status);
    Optional<FlexiblePlan> findById(Long id);
    List<FlexiblePlan> findByStatus(PlanStatus status);
}