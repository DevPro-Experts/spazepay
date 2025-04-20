package com.spazepay.repository;

import com.spazepay.model.enums.GroupMemberStatus;
import com.spazepay.model.savings.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroupIdAndStatus(Long groupId, GroupMemberStatus status);
    long countByGroupIdAndStatus(Long groupId, GroupMemberStatus status);

    boolean existsByGroupIdAndUserId(Long groupId, Long id);

    Optional<GroupMember> findByGroupIdAndPayoutOrder(Long groupId, int payoutOrder);

    List<GroupMember> findByGroupIdAndPayoutOrderGreaterThan(Long groupId, int leavingPayoutOrder);

    List<GroupMember> findByUserId(Long id);

    Optional<Object> findByGroupIdAndUserId(Long groupId, Long id);
}