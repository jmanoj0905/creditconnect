package com.creditconnect.dto;

import com.creditconnect.model.LoanOffer;

import java.math.BigDecimal;

public record LoanOfferResponse(Long offerId,
                                Long lenderId,
                                String lender,
                                BigDecimal amount,
                                BigDecimal interestRate,
                                String status) {

    public static LoanOfferResponse from(LoanOffer offer) {
        return new LoanOfferResponse(offer.getId(), offer.getLender().getId(),
                offer.getLender().getName(), offer.getOfferedAmount(),
                offer.getInterestRate(), offer.getStatus().name());
    }
}
