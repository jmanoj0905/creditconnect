package com.creditconnect.service;

import com.creditconnect.dto.LoanResponse;
import com.creditconnect.exception.InvalidLoanStateException;
import com.creditconnect.exception.ResourceNotFoundException;
import com.creditconnect.model.ApplicationStatus;
import com.creditconnect.model.Lender;
import com.creditconnect.model.LoanApplication;
import com.creditconnect.model.LoanOffer;
import com.creditconnect.model.OfferStatus;
import com.creditconnect.model.Repayment;
import com.creditconnect.model.User;
import com.creditconnect.repository.LenderRepository;
import com.creditconnect.repository.LoanApplicationRepository;
import com.creditconnect.repository.LoanOfferRepository;
import com.creditconnect.repository.LoanRepository;
import com.creditconnect.repository.RepaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private LoanOfferRepository offerRepository;

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RepaymentRepository repaymentRepository;

    @Mock
    private LenderRepository lenderRepository;

    private OfferService offerService;

    private Lender lender;
    private LoanApplication application;

    @BeforeEach
    void setUp() {
        offerService = new OfferService(offerRepository, applicationRepository, loanRepository,
                repaymentRepository, lenderRepository, new EligibilityService());

        lender = new Lender("HDFC Bank", new BigDecimal("5000000.00"), new BigDecimal("40000.00"),
                new BigDecimal("12.00"), 36);
        User user = new User("Rahul Sharma", "rahul@example.com", new BigDecimal("60000.00"));
        application = new LoanApplication(user, new BigDecimal("50000.00"), 12);
        application.setStatus(ApplicationStatus.OFFERS_AVAILABLE);
    }

    @Test
    void acceptingAnOfferCreatesLoanAndSchedule() {
        LoanOffer chosen = offer(lender, "12.00");
        LoanOffer sibling = offer(lender, "14.00");
        given(offerRepository.findById(1L)).willReturn(Optional.of(chosen));
        given(applicationRepository.findByIdForUpdate(any())).willReturn(Optional.of(application));
        given(offerRepository.findByApplicationIdOrderByInterestRateAsc(any()))
                .willReturn(List.of(chosen, sibling));
        given(loanRepository.save(any())).willAnswer(call -> call.getArgument(0));
        given(repaymentRepository.saveAll(anyList())).willAnswer(call -> call.getArgument(0));

        LoanResponse response = offerService.acceptOffer(1L);

        assertThat(response.emiAmount()).isEqualByComparingTo("4442.44");
        assertThat(response.tenureMonths()).isEqualTo(12);
        assertThat(response.pendingInstalments()).isEqualTo(12);
        assertThat(chosen.getStatus()).isEqualTo(OfferStatus.ACCEPTED);
        assertThat(sibling.getStatus()).isEqualTo(OfferStatus.REJECTED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(lender.getAvailableAmount()).isEqualByComparingTo("4950000.00");
    }

    @Test
    void generatedScheduleIsSequentialAndSumsToTotal() {
        LoanOffer chosen = offer(lender, "12.00");
        given(offerRepository.findById(1L)).willReturn(Optional.of(chosen));
        given(applicationRepository.findByIdForUpdate(any())).willReturn(Optional.of(application));
        given(offerRepository.findByApplicationIdOrderByInterestRateAsc(any()))
                .willReturn(List.of(chosen));
        given(loanRepository.save(any())).willAnswer(call -> call.getArgument(0));
        given(repaymentRepository.saveAll(anyList())).willAnswer(call -> call.getArgument(0));

        offerService.acceptOffer(1L);

        ArgumentCaptor<List<Repayment>> captor = ArgumentCaptor.captor();
        verify(repaymentRepository).saveAll(captor.capture());
        List<Repayment> schedule = captor.getValue();

        assertThat(schedule).hasSize(12);
        assertThat(schedule).extracting(Repayment::getInstalmentNumber)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(schedule.getFirst().getDueDate()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(schedule.getLast().getDueDate()).isEqualTo(LocalDate.now().plusMonths(12));
        assertThat(schedule.stream().map(Repayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(new BigDecimal("4442.44").multiply(new BigDecimal("12")));
    }

    @Test
    void rejectsUnknownOffer() {
        given(offerRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.acceptOffer(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void rejectsOfferThatIsNoLongerAvailable() {
        LoanOffer chosen = offer(lender, "12.00");
        chosen.setStatus(OfferStatus.REJECTED);
        given(offerRepository.findById(1L)).willReturn(Optional.of(chosen));
        given(applicationRepository.findByIdForUpdate(any())).willReturn(Optional.of(application));

        assertThatThrownBy(() -> offerService.acceptOffer(1L))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void rejectsSecondAcceptOnTheSameApplication() {
        LoanOffer chosen = offer(lender, "12.00");
        application.setStatus(ApplicationStatus.ACCEPTED);
        given(offerRepository.findById(1L)).willReturn(Optional.of(chosen));
        given(applicationRepository.findByIdForUpdate(any())).willReturn(Optional.of(application));

        assertThatThrownBy(() -> offerService.acceptOffer(1L))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("not accepting offers");
    }

    private LoanOffer offer(Lender offerLender, String rate) {
        return new LoanOffer(application, offerLender, new BigDecimal("50000.00"),
                new BigDecimal(rate));
    }
}
