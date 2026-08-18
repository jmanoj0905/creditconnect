package com.creditconnect.repository;

import com.creditconnect.model.LoanApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByUserIdOrderByIdDesc(Long userId);

    // Issues SELECT ... FOR UPDATE so concurrent accepts on the same application
    // queue behind each other instead of both seeing OFFERS_AVAILABLE.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoanApplication a where a.id = :id")
    Optional<LoanApplication> findByIdForUpdate(@Param("id") Long id);
}
