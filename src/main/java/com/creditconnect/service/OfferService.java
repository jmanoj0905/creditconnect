package com.creditconnect.service;

import com.creditconnect.dto.LoanResponse;
import com.creditconnect.exception.InvalidLoanStateException;
import com.creditconnect.exception.ResourceNotFoundException;
import com.creditconnect.model.ApplicationStatus;
import com.creditconnect.model.Lender;
import com.creditconnect.model.Loan;
import com.creditconnect.model.LoanApplication;
import com.creditconnect.model.LoanOffer;
import com.creditconnect.model.OfferStatus;
import com.creditconnect.model.Repayment;
import com.creditconnect.repository.LenderRepository;
import com.creditconnect.repository.LoanApplicationRepository;
import com.creditconnect.repository.LoanOfferRepository;
import com.creditconnect.repository.LoanRepository;
import com.creditconnect.repository.RepaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OfferService {

    private final LoanOfferRepository offerRepository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final LenderRepository lenderRepository;
    private final EligibilityService eligibilityService;

    public OfferService(LoanOfferRepository offerRepository,
                        LoanApplicationRepository applicationRepository,
                        LoanRepository loanRepository,
                        RepaymentRepository repaymentRepository,
                        LenderRepository lenderRepository,
                        EligibilityService eligibilityService) {
        this.offerRepository = offerRepository;
        this.applicationRepository = applicationRepository;
        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
        this.lenderRepository = lenderRepository;
        this.eligibilityService = eligibilityService;
    }

    /**
     * Two customers can hit this endpoint for the same application at the same
     * moment. The application row is locked before anything is read or written,
     * so the second request only starts once the first has committed and will
     * find the application already ACCEPTED.
     */
    @Transactional
    public LoanResponse acceptOffer(Long offerId) {
        LoanOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer " + offerId + " not found"));

        Long applicationId = offer.getApplication().getId();
        LoanApplication application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan application " + applicationId + " not found"));

        if (application.getStatus() != ApplicationStatus.OFFERS_AVAILABLE) {
            throw new InvalidLoanStateException("Loan application " + application.getId()
                    + " is not accepting offers (status " + application.getStatus() + ")");
        }
        if (offer.getStatus() != OfferStatus.AVAILABLE) {
            throw new InvalidLoanStateException("Offer " + offerId + " is no longer available");
        }

        Lender lender = offer.getLender();
        if (lender.getAvailableAmount().compareTo(offer.getOfferedAmount()) < 0) {
            throw new InvalidLoanStateException(
                    "Lender " + lender.getName() + " no longer has funds for this offer");
        }

        for (LoanOffer sibling : offerRepository
                .findByApplicationIdOrderByInterestRateAsc(application.getId())) {
            if (sibling != offer) {
                sibling.setStatus(OfferStatus.REJECTED);
            }
        }
        offer.setStatus(OfferStatus.ACCEPTED);

        lender.setAvailableAmount(lender.getAvailableAmount().subtract(offer.getOfferedAmount()));
        lenderRepository.save(lender);

        application.setStatus(ApplicationStatus.ACCEPTED);

        BigDecimal emi = eligibilityService.monthlyInstalment(offer.getOfferedAmount(),
                offer.getInterestRate(), application.getTenureMonths());
        Loan loan = loanRepository.save(new Loan(application, lender, offer.getOfferedAmount(),
                offer.getInterestRate(), application.getTenureMonths(), emi));

        repaymentRepository.saveAll(buildSchedule(loan, emi));

        return LoanResponse.from(loan, application.getTenureMonths());
    }

    private List<Repayment> buildSchedule(Loan loan, BigDecimal emi) {
        LocalDate start = LocalDate.now();
        List<Repayment> schedule = new ArrayList<>();
        for (int instalment = 1; instalment <= loan.getTenureMonths(); instalment++) {
            schedule.add(new Repayment(loan, instalment, start.plusMonths(instalment), emi));
        }
        return schedule;
    }
}
