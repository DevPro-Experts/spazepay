package com.spazepay.repository;

import com.spazepay.model.savings.GroupContribution;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupContributionRepository extends JpaRepository<GroupContribution, Long> {
    List<GroupContribution> findByGroupIdAndCycleNumber(Long groupId, int cycleNumber);

    boolean existsByGroupIdAndUserIdAndCycleNumber(Long groupId, Long id, @NotBlank(message = "Cycle Number is required") int cycleNumber);

    long countByGroupIdAndCycleNumber(Long groupId, int cycleNumber);
}