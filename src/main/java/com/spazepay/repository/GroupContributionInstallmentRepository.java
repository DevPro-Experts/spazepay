package com.spazepay.repository;

import com.spazepay.model.savings.GroupContributionInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.util.List;

public interface GroupContributionInstallmentRepository extends JpaRepository<GroupContributionInstallment, Long> {
    List<GroupContributionInstallment> findByGroupIdAndUserIdAndCycleNumber(Long groupId, Long userId, int cycleNumber);
    List<GroupContributionInstallment> findByGroupIdAndCycleNumber(Long groupId, int cycleNumber);
}