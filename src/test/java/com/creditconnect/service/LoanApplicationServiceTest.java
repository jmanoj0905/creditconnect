package com.creditconnect.service;

import com.creditconnect.dto.LoanApplicationRequest;
import com.creditconnect.dto.LoanApplicationResponse;
import com.creditconnect.model.ApplicationStatus;
import com.creditconnect.model.Lender;
import com.creditconnect.model.LoanOffer;
import com.creditconnect.model.User;
import com.creditconnect.repository.LenderRepository;
import com.creditconnect.repository.LoanApplicationRepository;
import com.creditconnect.repository.LoanOfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private LoanOfferRepository offerRepository;

    @Mock
    private LenderRepository lenderRepository;

    @Mock
    private UserService userService;

    private LoanApplicationService loanApplicationService;

    @BeforeEach
    void setUp() {
        loanApplicationService = new LoanApplicationService(applicationRepository, offerRepository,
                lenderRepository, userService, new EligibilityService());
    }

    @Test
    void rejectsApplicationAboveEligibilityLimit() {
        given(userService.requireUser(1L)).willReturn(user("60000"));
        given(applicationRepository.save(any())).willAnswer(call -> call.getArgument(0));

        LoanApplicationResponse response = loanApplicationService.apply(
                new LoanApplicationRequest(1L, new BigDecimal("400000"), 12));

        assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED.name());
        assertThat(response.rejectionReason()).contains("300000");
        assertThat(response.offerCount()).isZero();
        verify(offerRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsApplicationWhenNoLenderMatches() {
        given(userService.requireUser(1L)).willReturn(user("60000"));
        given(applicationRepository.save(any())).willAnswer(call -> call.getArgument(0));
        given(lenderRepository.findByActiveTrue()).willReturn(
                List.of(lender("HDFC Bank", "10000", "40000", "12.00", 36)));

        LoanApplicationResponse response = loanApplicationService.apply(
                new LoanApplicationRequest(1L, new BigDecimal("50000"), 12));

        assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED.name());
        assertThat(response.rejectionReason()).isEqualTo("No lender available for this request");
        verify(offerRepository, never()).saveAll(anyList());
    }

    @Test
    void generatesOneOfferPerMatchingLender() {
        given(userService.requireUser(1L)).willReturn(user("60000"));
        given(applicationRepository.save(any())).willAnswer(call -> call.getArgument(0));
        given(lenderRepository.findByActiveTrue()).willReturn(List.of(
                lender("HDFC Bank", "5000000", "40000", "12.00", 36),
                lender("ABC NBFC", "2000000", "25000", "13.50", 24),
                lender("XYZ NBFC", "1000000", "15000", "15.00", 6)));
        given(offerRepository.saveAll(anyList())).willAnswer(call -> call.getArgument(0));

        LoanApplicationResponse response = loanApplicationService.apply(
                new LoanApplicationRequest(1L, new BigDecimal("50000"), 12));

        assertThat(response.status()).isEqualTo(ApplicationStatus.OFFERS_AVAILABLE.name());
        assertThat(response.offerCount()).isEqualTo(2);

        ArgumentCaptor<List<LoanOffer>> captor = ArgumentCaptor.captor();
        verify(offerRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(offer -> offer.getLender().getName())
                .containsExactly("HDFC Bank", "ABC NBFC");
    }

    @Test
    void appliesLeverageSurchargeToGeneratedOffers() {
        given(userService.requireUser(1L)).willReturn(user("60000"));
        given(applicationRepository.save(any())).willAnswer(call -> call.getArgument(0));
        given(lenderRepository.findByActiveTrue()).willReturn(
                List.of(lender("HDFC Bank", "5000000", "40000", "12.00", 36)));
        given(offerRepository.saveAll(anyList())).willAnswer(call -> call.getArgument(0));

        loanApplicationService.apply(new LoanApplicationRequest(1L, new BigDecimal("250000"), 24));

        ArgumentCaptor<List<LoanOffer>> captor = ArgumentCaptor.captor();
        verify(offerRepository).saveAll(captor.capture());
        assertThat(captor.getValue().getFirst().getInterestRate()).isEqualByComparingTo("13.50");
    }

    private User user(String income) {
        return new User("Rahul Sharma", "rahul@example.com", new BigDecimal(income));
    }

    private Lender lender(String name, String pool, String minIncome, String rate, int maxTenure) {
        return new Lender(name, new BigDecimal(pool), new BigDecimal(minIncome),
                new BigDecimal(rate), maxTenure);
    }
}
