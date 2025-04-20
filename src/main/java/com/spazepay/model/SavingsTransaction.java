package com.spazepay.model;

import com.spazepay.model.enums.SavingsType;
import com.spazepay.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "savings_transactions")
@Data
public class SavingsTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private SavingsType planType;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    private String source;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    private Instant timestamp;

    public SavingsTransaction() {
        this.timestamp = Instant.now();
    }
}