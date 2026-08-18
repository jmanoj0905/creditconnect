package com.creditconnect.service;

import com.creditconnect.dto.RepaymentResponse;
import com.creditconnect.exception.InvalidLoanStateException;
import com.creditconnect.exception.ResourceNotFoundException;
import com.creditconnect.model.Lender;
import com.creditconnect.model.Loan;
import com.creditconnect.model.LoanApplication;
import com.creditconnect.model.LoanStatus;
import com.creditconnect.model.Repayment;
import com.creditconnect.model.RepaymentStatus;
import com.creditconnect.model.User;
import com.creditconnect.repository.LoanRepository;
import com.creditconnect.repository.RepaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RepaymentRepository repaymentRepository;

    private RepaymentService repaymentService;

    private Loan loan;

    @BeforeEach
    void setUp() {
        repaymentService = new RepaymentService(loanRepository, repaymentRepository);

        User user = new User("Rahul Sharma", "rahul@example.com", new BigDecimal("60000.00"));
        Lender lender = new Lender("HDFC Bank", new BigDecimal("5000000.00"),
                new BigDecimal("40000.00"), new BigDecimal("12.00"), 36);
        LoanApplication application = new LoanApplication(user, new BigDecimal("50000.00"), 12);
        loan = new Loan(application, lender, new BigDecimal("50000.00"), new BigDecimal("12.00"),
                12, new BigDecimal("4442.44"));
    }

    @Test
    void paysTheNextPendingInstalment() {
        Repayment first = new Repayment(loan, 1, LocalDate.now().plusMonths(1),
                new BigDecimal("4442.44"));
        given(loanRepository.findByApplicationId(1L)).willReturn(Optional.of(loan));
        given(repaymentRepository.findFirstByLoanIdAndStatusOrderByInstalmentNumberAsc(
                any(), eq(RepaymentStatus.PENDING))).willReturn(Optional.of(first));
        given(repaymentRepository.countByLoanIdAndStatus(any(), eq(RepaymentStatus.PENDING)))
                .willReturn(11L);

        RepaymentResponse response = repaymentService.repayNextInstalment(1L);

        assertThat(first.getStatus()).isEqualTo(RepaymentStatus.PAID);
        assertThat(first.getPaidAt()).isNotNull();
        assertThat(response.instalmentNumber()).isEqualTo(1);
        assertThat(response.remainingInstalments()).isEqualTo(11);
        assertThat(response.loanStatus()).isEqualTo(LoanStatus.ACTIVE.name());
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    void completesTheLoanOnTheFinalInstalment() {
        Repayment last = new Repayment(loan, 12, LocalDate.now().plusMonths(12),
                new BigDecimal("4442.44"));
        given(loanRepository.findByApplicationId(1L)).willReturn(Optional.of(loan));
        given(repaymentRepository.findFirstByLoanIdAndStatusOrderByInstalmentNumberAsc(
                any(), eq(RepaymentStatus.PENDING))).willReturn(Optional.of(last));
        given(repaymentRepository.countByLoanIdAndStatus(any(), eq(RepaymentStatus.PENDING)))
                .willReturn(0L);

        RepaymentResponse response = repaymentService.repayNextInstalment(1L);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.COMPLETED);
        assertThat(response.loanStatus()).isEqualTo(LoanStatus.COMPLETED.name());
        assertThat(response.remainingInstalments()).isZero();
    }

    @Test
    void rejectsRepaymentOnACompletedLoan() {
        loan.setStatus(LoanStatus.COMPLETED);
        given(loanRepository.findByApplicationId(1L)).willReturn(Optional.of(loan));

        assertThatThrownBy(() -> repaymentService.repayNextInstalment(1L))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void rejectsRepaymentWhenApplicationHasNoLoan() {
        given(loanRepository.findByApplicationId(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> repaymentService.repayNextInstalment(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("7");
    }
}
