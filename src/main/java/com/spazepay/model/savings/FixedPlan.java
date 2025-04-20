package com.spazepay.model.savings;

import com.spazepay.model.User;
import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.SavingsType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_plans")
@Data
public class FixedPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String name;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    @Column(name = "principal_balance", precision = 15, scale = 2)
    private BigDecimal principalBalance;

    @Column(name = "lock_period_months")
    private int lockPeriodMonths;

    @Enumerated(EnumType.STRING)
    private SavingsType type;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "matured_at")
    private LocalDateTime maturedAt;

    @Column(precision = 19, scale = 2)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    public FixedPlan() {
        this.createdAt = Instant.now();
        this.status = PlanStatus.ACTIVE;
    }
}