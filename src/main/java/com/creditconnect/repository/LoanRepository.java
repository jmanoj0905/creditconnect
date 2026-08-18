package com.creditconnect.repository;

import com.creditconnect.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByApplicationUserIdOrderByIdDesc(Long userId);

    Optional<Loan> findByApplicationId(Long applicationId);
}
