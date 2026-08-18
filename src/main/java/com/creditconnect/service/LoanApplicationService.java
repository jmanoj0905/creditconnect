package com.creditconnect.service;

import com.creditconnect.dto.LoanApplicationRequest;
import com.creditconnect.dto.LoanApplicationResponse;
import com.creditconnect.dto.LoanOfferResponse;
import com.creditconnect.exception.ResourceNotFoundException;
import com.creditconnect.model.ApplicationStatus;
import com.creditconnect.model.Lender;
import com.creditconnect.model.LoanApplication;
import com.creditconnect.model.LoanOffer;
import com.creditconnect.model.User;
import com.creditconnect.repository.LenderRepository;
import com.creditconnect.repository.LoanApplicationRepository;
import com.creditconnect.repository.LoanOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final LoanOfferRepository offerRepository;
    private final LenderRepository lenderRepository;
    private final UserService userService;
    private final EligibilityService eligibilityService;

    public LoanApplicationService(LoanApplicationRepository applicationRepository,
                                  LoanOfferRepository offerRepository,
                                  LenderRepository lenderRepository,
                                  UserService userService,
                                  EligibilityService eligibilityService) {
        this.applicationRepository = applicationRepository;
        this.offerRepository = offerRepository;
        this.lenderRepository = lenderRepository;
        this.userService = userService;
        this.eligibilityService = eligibilityService;
    }

    @Transactional
    public LoanApplicationResponse apply(LoanApplicationRequest request) {
        User user = userService.requireUser(request.userId());
        LoanApplication application = applicationRepository.save(
                new LoanApplication(user, request.amount(), request.tenureMonths()));

        BigDecimal maxEligible = eligibilityService.maxEligibleAmount(user.getMonthlyIncome());
        if (request.amount().compareTo(maxEligible) > 0) {
            return reject(application,
                    "Requested amount exceeds eligibility limit of " + maxEligible.toPlainString());
        }

        List<LoanOffer> offers = buildOffers(application, user);
        if (offers.isEmpty()) {
            return reject(application, "No lender available for this request");
        }

        offerRepository.saveAll(offers);
        application.setStatus(ApplicationStatus.OFFERS_AVAILABLE);
        return LoanApplicationResponse.from(application, offers.size());
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponse getApplication(Long applicationId) {
        LoanApplication application = requireApplication(applicationId);
        int offerCount = offerRepository
                .findByApplicationIdOrderByInterestRateAsc(applicationId).size();
        return LoanApplicationResponse.from(application, offerCount);
    }

    @Transactional(readOnly = true)
    public List<LoanOfferResponse> getOffers(Long applicationId) {
        requireApplication(applicationId);
        return offerRepository.findByApplicationIdOrderByInterestRateAsc(applicationId).stream()
                .map(LoanOfferResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanApplication requireApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan application " + applicationId + " not found"));
    }

    private List<LoanOffer> buildOffers(LoanApplication application, User user) {
        List<LoanOffer> offers = new ArrayList<>();
        for (Lender lender : lenderRepository.findByActiveTrue()) {
            if (!eligibilityService.canLenderServe(lender, user.getMonthlyIncome(),
                    application.getAmount(), application.getTenureMonths())) {
                continue;
            }
            BigDecimal rate = eligibilityService.priceOffer(
                    lender, user.getMonthlyIncome(), application.getAmount());
            offers.add(new LoanOffer(application, lender, application.getAmount(), rate));
        }
        return offers;
    }

    private LoanApplicationResponse reject(LoanApplication application, String reason) {
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(reason);
        return LoanApplicationResponse.from(application, 0);
    }
}
