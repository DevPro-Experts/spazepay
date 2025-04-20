package com.spazepay.model.savings;

import com.spazepay.model.User;
import com.spazepay.model.enums.GroupMemberStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "group_members")
@Data
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private GroupPlan group;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private int payoutOrder;

    @Enumerated(EnumType.STRING)
    private GroupMemberStatus status;
}