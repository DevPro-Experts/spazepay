package com.spazepay.model;

import com.spazepay.model.enums.InterestHandling;
import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.SavingsType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "savings_plans")
@Data
public class SavingsPlan {
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

    @Enumerated(EnumType.STRING)
    private SavingsType type; // To differentiate Flexible, Target, Fixed

    @Column(precision = 19, scale = 2)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    // Constructors
    public SavingsPlan() {
        this.createdAt = Instant.now();
        this.status = PlanStatus.ACTIVE;
    }
}