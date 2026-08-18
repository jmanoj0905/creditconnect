package com.creditconnect.repository;

import com.creditconnect.model.LoanOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanOfferRepository extends JpaRepository<LoanOffer, Long> {

    List<LoanOffer> findByApplicationIdOrderByInterestRateAsc(Long applicationId);
}
