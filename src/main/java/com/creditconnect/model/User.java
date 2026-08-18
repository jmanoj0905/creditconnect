package com.creditconnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "monthly_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public User(String name, String email, BigDecimal monthlyIncome) {
        this.name = name;
        this.email = email;
        this.monthlyIncome = monthlyIncome;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
