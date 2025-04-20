package com.spazepay.model.savings;

import com.spazepay.model.User;
import com.spazepay.model.enums.InterestHandling;
import com.spazepay.model.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "flexible_plans")
@Data
public class FlexiblePlan {
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

    @Enumerated(EnumType.STRING)
    private InterestHandling interestHandling;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "matured_at")
    private LocalDateTime maturedAt;

    @Column(precision = 19, scale = 2)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    public FlexiblePlan() {
        this.createdAt = Instant.now();
        this.status = PlanStatus.ACTIVE;
    }
}