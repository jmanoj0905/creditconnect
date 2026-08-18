package com.creditconnect.service;

import com.creditconnect.dto.LoanResponse;
import com.creditconnect.dto.RepaymentResponse;
import com.creditconnect.exception.InvalidLoanStateException;
import com.creditconnect.exception.ResourceNotFoundException;
import com.creditconnect.model.Loan;
import com.creditconnect.model.LoanStatus;
import com.creditconnect.model.Repayment;
import com.creditconnect.model.RepaymentStatus;
import com.creditconnect.repository.LoanRepository;
import com.creditconnect.repository.RepaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepaymentService {

    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;

    public RepaymentService(LoanRepository loanRepository,
                            RepaymentRepository repaymentRepository) {
        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
    }

    @Transactional
    public RepaymentResponse repayNextInstalment(Long applicationId) {
        Loan loan = loanRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No loan found for application " + applicationId));

        if (loan.getStatus() == LoanStatus.COMPLETED) {
            throw new InvalidLoanStateException("Loan " + loan.getId() + " is already completed");
        }

        Repayment due = repaymentRepository
                .findFirstByLoanIdAndStatusOrderByInstalmentNumberAsc(
                        loan.getId(), RepaymentStatus.PENDING)
                .orElseThrow(() -> new InvalidLoanStateException(
                        "Loan " + loan.getId() + " has no pending instalment"));

        due.setStatus(RepaymentStatus.PAID);
        due.setPaidAt(LocalDateTime.now());
        repaymentRepository.save(due);

        long remaining = repaymentRepository.countByLoanIdAndStatus(
                loan.getId(), RepaymentStatus.PENDING);
        if (remaining == 0) {
            loan.setStatus(LoanStatus.COMPLETED);
            loanRepository.save(loan);
        }

        return new RepaymentResponse(due.getId(), due.getInstalmentNumber(), due.getDueDate(),
                due.getAmount(), due.getStatus().name(), remaining, loan.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> loansForUser(Long userId) {
        return loanRepository.findByApplicationUserIdOrderByIdDesc(userId).stream()
                .map(loan -> LoanResponse.from(loan, repaymentRepository.countByLoanIdAndStatus(
                        loan.getId(), RepaymentStatus.PENDING)))
                .toList();
    }
}
