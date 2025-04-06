package com.spazepay.model;

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

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private SavingsPlan plan;

    private String month; // e.g., "2025-04"

    private int withdrawalCount;

    private boolean interestForfeited;

}