package com.spazepay.repository;

import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.savings.GroupPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupPlanRepository extends JpaRepository<GroupPlan, Long> {
    List<GroupPlan> findByCreatorIdAndStatus(Long creatorId, PlanStatus status);
    List<GroupPlan> findByStatus(PlanStatus status);
}