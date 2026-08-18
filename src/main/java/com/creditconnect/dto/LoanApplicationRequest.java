package com.creditconnect.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanApplicationRequest(
        @NotNull(message = "userId is required")
        Long userId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "1000.00", message = "amount must be at least 1000")
        BigDecimal amount,

        @Min(value = 1, message = "tenureMonths must be at least 1")
        @Max(value = 60, message = "tenureMonths must be at most 60")
        int tenureMonths) {
}
