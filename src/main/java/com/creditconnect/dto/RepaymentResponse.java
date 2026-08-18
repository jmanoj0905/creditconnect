package com.creditconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepaymentResponse(Long repaymentId,
                                int instalmentNumber,
                                LocalDate dueDate,
                                BigDecimal amount,
                                String status,
                                long remainingInstalments,
                                String loanStatus) {
}
