package com.creditconnect;

import com.creditconnect.dto.CreateUserRequest;
import com.creditconnect.dto.LoanApplicationRequest;
import com.creditconnect.dto.LoanApplicationResponse;
import com.creditconnect.dto.LoanOfferResponse;
import com.creditconnect.dto.LoanResponse;
import com.creditconnect.dto.RepaymentResponse;
import com.creditconnect.dto.UserResponse;
import com.creditconnect.model.ApplicationStatus;
import com.creditconnect.model.LoanStatus;
import com.creditconnect.service.LoanApplicationService;
import com.creditconnect.service.OfferService;
import com.creditconnect.service.RepaymentService;
import com.creditconnect.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Sql("/test-lenders.sql")
class LoanJourneyIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private RepaymentService repaymentService;

    @Test
    void runsTheFullJourneyFromApplicationToClosure() {
        UserResponse user = userService.create(new CreateUserRequest(
                "Rahul Sharma", "rahul.journey@example.com", new BigDecimal("60000")));

        LoanApplicationResponse application = loanApplicationService.apply(
                new LoanApplicationRequest(user.id(), new BigDecimal("50000"), 12));
        assertThat(application.status()).isEqualTo(ApplicationStatus.OFFERS_AVAILABLE.name());
        assertThat(application.offerCount()).isEqualTo(3);

        List<LoanOfferResponse> offers =
                loanApplicationService.getOffers(application.applicationId());
        assertThat(offers).extracting(LoanOfferResponse::interestRate)
                .isSortedAccordingTo(BigDecimal::compareTo);

        LoanResponse loan = offerService.acceptOffer(offers.getFirst().offerId());
        assertThat(loan.interestRate()).isEqualByComparingTo("12.00");
        assertThat(loan.emiAmount()).isEqualByComparingTo("4442.44");
        assertThat(loan.status()).isEqualTo(LoanStatus.ACTIVE.name());

        List<LoanOfferResponse> afterAccept =
                loanApplicationService.getOffers(application.applicationId());
        assertThat(afterAccept).filteredOn(offer -> offer.status().equals("ACCEPTED")).hasSize(1);
        assertThat(afterAccept).filteredOn(offer -> offer.status().equals("REJECTED")).hasSize(2);

        RepaymentResponse last = null;
        for (int instalment = 1; instalment <= 12; instalment++) {
            last = repaymentService.repayNextInstalment(application.applicationId());
        }

        assertThat(last).isNotNull();
        assertThat(last.remainingInstalments()).isZero();
        assertThat(last.loanStatus()).isEqualTo(LoanStatus.COMPLETED.name());

        assertThat(repaymentService.loansForUser(user.id()))
                .singleElement()
                .satisfies(entry -> assertThat(entry.status())
                        .isEqualTo(LoanStatus.COMPLETED.name()));
    }

    @Test
    void rejectsApplicationOverTheEligibilityLimit() {
        UserResponse user = userService.create(new CreateUserRequest(
                "Priya Nair", "priya.journey@example.com", new BigDecimal("30000")));

        LoanApplicationResponse application = loanApplicationService.apply(
                new LoanApplicationRequest(user.id(), new BigDecimal("200000"), 12));

        assertThat(application.status()).isEqualTo(ApplicationStatus.REJECTED.name());
        assertThat(application.rejectionReason()).contains("60000");
        assertThat(loanApplicationService.getOffers(application.applicationId())).isEmpty();
    }
}
