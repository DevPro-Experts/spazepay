package com.spazepay.model;

import com.spazepay.model.enums.SavingsType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "monthly_activity")
@Data
public class MonthlyActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "plan_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SavingsType planType;

    private String month;

    @Column(name = "withdrawal_count")
    private int withdrawalCount;

    @Column(name = "interest_forfeited")
    private boolean interestForfeited;
}