package com.creditconnect.repository;

import com.creditconnect.model.Repayment;
import com.creditconnect.model.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    Optional<Repayment> findFirstByLoanIdAndStatusOrderByInstalmentNumberAsc(
            Long loanId, RepaymentStatus status);

    long countByLoanIdAndStatus(Long loanId, RepaymentStatus status);

    List<Repayment> findByLoanIdOrderByInstalmentNumberAsc(Long loanId);
}
