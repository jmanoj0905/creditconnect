package com.creditconnect.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EntityMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsFullLoanGraph() {
        User user = entityManager.persist(
                new User("Rahul Sharma", "rahul@example.com", new BigDecimal("60000.00")));
        Lender lender = entityManager.persist(
                new Lender("HDFC Bank", new BigDecimal("5000000.00"), new BigDecimal("40000.00"),
                        new BigDecimal("12.00"), 36));
        LoanApplication application = entityManager.persist(
                new LoanApplication(user, new BigDecimal("50000.00"), 12));
        LoanOffer offer = entityManager.persist(
                new LoanOffer(application, lender, new BigDecimal("50000.00"), new BigDecimal("12.00")));
        Loan loan = entityManager.persist(
                new Loan(application, lender, new BigDecimal("50000.00"), new BigDecimal("12.00"),
                        12, new BigDecimal("4442.44")));
        Repayment repayment = entityManager.persist(
                new Repayment(loan, 1, LocalDate.of(2026, 9, 18), new BigDecimal("4442.44")));

        entityManager.flush();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.AVAILABLE);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(repayment.getStatus()).isEqualTo(RepaymentStatus.PENDING);
    }
}
