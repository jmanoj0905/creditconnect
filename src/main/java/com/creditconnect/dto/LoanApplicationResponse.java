package com.creditconnect.dto;

import com.creditconnect.model.LoanApplication;

import java.math.BigDecimal;

public record LoanApplicationResponse(Long applicationId,
                                      Long userId,
                                      BigDecimal amount,
                                      int tenureMonths,
                                      String status,
                                      String rejectionReason,
                                      int offerCount) {

    public static LoanApplicationResponse from(LoanApplication application, int offerCount) {
        return new LoanApplicationResponse(application.getId(), application.getUser().getId(),
                application.getAmount(), application.getTenureMonths(),
                application.getStatus().name(), application.getRejectionReason(), offerCount);
    }
}
