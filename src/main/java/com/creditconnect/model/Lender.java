package com.creditconnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "lender")
public class Lender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "available_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal availableAmount;

    @Column(name = "min_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal minIncome;

    @Column(name = "base_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal baseInterestRate;

    @Column(name = "max_tenure_months", nullable = false)
    private int maxTenureMonths;

    @Column(nullable = false)
    private boolean active = true;

    protected Lender() {
    }

    public Lender(String name, BigDecimal availableAmount, BigDecimal minIncome,
                  BigDecimal baseInterestRate, int maxTenureMonths) {
        this.name = name;
        this.availableAmount = availableAmount;
        this.minIncome = minIncome;
        this.baseInterestRate = baseInterestRate;
        this.maxTenureMonths = maxTenureMonths;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(BigDecimal availableAmount) {
        this.availableAmount = availableAmount;
    }

    public BigDecimal getMinIncome() {
        return minIncome;
    }

    public BigDecimal getBaseInterestRate() {
        return baseInterestRate;
    }

    public int getMaxTenureMonths() {
        return maxTenureMonths;
    }

    public boolean isActive() {
        return active;
    }
}
