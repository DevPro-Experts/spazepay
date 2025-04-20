package com.spazepay.model.savings;

import com.spazepay.model.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "group_invitations")
public class GroupInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private GroupPlan group;

    @ManyToOne
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted", nullable = false)
    private boolean accepted = false;
}