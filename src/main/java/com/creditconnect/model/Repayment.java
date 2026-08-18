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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repayment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"loan_id", "instalment_number"}))
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "instalment_number", nullable = false)
    private int instalmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RepaymentStatus status = RepaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    protected Repayment() {
    }

    public Repayment(Loan loan, int instalmentNumber, LocalDate dueDate, BigDecimal amount) {
        this.loan = loan;
        this.instalmentNumber = instalmentNumber;
        this.dueDate = dueDate;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public Loan getLoan() {
        return loan;
    }

    public int getInstalmentNumber() {
        return instalmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public RepaymentStatus getStatus() {
        return status;
    }

    public void setStatus(RepaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
