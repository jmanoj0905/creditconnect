package com.creditconnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lender_id", nullable = false)
    private Lender lender;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal principal;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(name = "emi_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal emiAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(name = "disbursed_at", nullable = false)
    private LocalDateTime disbursedAt;

    protected Loan() {
    }

    public Loan(LoanApplication application, Lender lender, BigDecimal principal,
                BigDecimal interestRate, int tenureMonths, BigDecimal emiAmount) {
        this.application = application;
        this.lender = lender;
        this.principal = principal;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
        this.emiAmount = emiAmount;
    }

    @PrePersist
    void onCreate() {
        this.disbursedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LoanApplication getApplication() {
        return application;
    }

    public Lender getLender() {
        return lender;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }
}
