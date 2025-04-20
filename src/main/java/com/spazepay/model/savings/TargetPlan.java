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
@Table(name = "target_plans")
@Data
public class TargetPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String name;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    @Enumerated(EnumType.STRING)
    private SavingsType type;

    @Column(name = "principal_balance", precision = 15, scale = 2)
    private BigDecimal principalBalance;

    @Column(name = "target_amount", precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(precision = 19, scale = 2)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    private boolean autoDebitEnabled;

    private String autoDebitFrequency; // e.g., WEEKLY, MONTHLY

    public TargetPlan() {
        this.createdAt = Instant.now();
        this.status = PlanStatus.ACTIVE;
    }
}