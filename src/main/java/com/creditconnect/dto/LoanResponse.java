package com.creditconnect.dto;

import com.creditconnect.model.Loan;

import java.math.BigDecimal;

public record LoanResponse(Long loanId,
                           Long applicationId,
                           String lender,
                           BigDecimal principal,
                           BigDecimal interestRate,
                           int tenureMonths,
                           BigDecimal emiAmount,
                           String status,
                           long pendingInstalments) {

    public static LoanResponse from(Loan loan, long pendingInstalments) {
        return new LoanResponse(loan.getId(), loan.getApplication().getId(),
                loan.getLender().getName(), loan.getPrincipal(), loan.getInterestRate(),
                loan.getTenureMonths(), loan.getEmiAmount(), loan.getStatus().name(),
                pendingInstalments);
    }
}
