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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_offer")
public class LoanOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lender_id", nullable = false)
    private Lender lender;

    @Column(name = "offered_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal offeredAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OfferStatus status = OfferStatus.AVAILABLE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LoanOffer() {
    }

    public LoanOffer(LoanApplication application, Lender lender,
                     BigDecimal offeredAmount, BigDecimal interestRate) {
        this.application = application;
        this.lender = lender;
        this.offeredAmount = offeredAmount;
        this.interestRate = interestRate;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public BigDecimal getOfferedAmount() {
        return offeredAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public void setStatus(OfferStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
