package com.spazepay.model;

import com.spazepay.model.enums.SavingsType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "daily_balances")
public class DailyBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "plan_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SavingsType planType;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal closingBalance = BigDecimal.ZERO;

    public DailyBalance(Long planId, SavingsType planType, LocalDate date, BigDecimal netBalance, BigDecimal closingBalance) {
        this.planId = planId;
        this.planType = planType;
        this.date = date;
        this.netBalance = netBalance;
        this.closingBalance = closingBalance;
    }
}