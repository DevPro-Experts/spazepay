package com.spazepay.model;

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

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private SavingsPlan plan;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netBalance = BigDecimal.ZERO; // Net inflow for the day

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal closingBalance = BigDecimal.ZERO; // End-of-day balance

    public DailyBalance(SavingsPlan plan, LocalDate date, BigDecimal netBalance, BigDecimal closingBalance) {
        this.plan = plan;
        this.date = date;
        this.netBalance = netBalance;
        this.closingBalance = closingBalance;
    }
}