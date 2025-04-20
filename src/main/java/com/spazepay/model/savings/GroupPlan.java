package com.spazepay.model.savings;

import com.spazepay.model.User;
import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.SavingsType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "group_plans")
@Data
public class GroupPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    private String name;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    @Column(name = "contribution_amount", precision = 15, scale = 2)
    private BigDecimal contributionAmount;

    private String contributionFrequency; // WEEKLY, MONTHLY

    @Column(name = "start_date")
    private LocalDate startDate;

    private int cycleCount;

    @Column(name = "created_at")
    private Instant createdAt;

    private int lastCompletedCycle;

    @Enumerated(EnumType.STRING)
    private SavingsType type;

    @Column(name = "service_fee_rate", precision = 5, scale = 2)
    private BigDecimal serviceFeeRate = new BigDecimal("0.005"); // 0.5%

    public GroupPlan() {
        this.createdAt = Instant.now();
        this.status = PlanStatus.PENDING;
    }
}