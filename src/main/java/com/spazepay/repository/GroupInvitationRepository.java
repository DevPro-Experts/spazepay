package com.spazepay.repository;

import com.spazepay.model.savings.GroupInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {
    Optional<GroupInvitation> findByToken(String token);
    boolean existsByGroupIdAndInviteeIdAndAcceptedFalse(Long groupId, Long inviteeId);
}