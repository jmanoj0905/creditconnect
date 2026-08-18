package com.creditconnect;

import com.creditconnect.dto.CreateUserRequest;
import com.creditconnect.dto.LoanApplicationRequest;
import com.creditconnect.dto.LoanApplicationResponse;
import com.creditconnect.dto.LoanOfferResponse;
import com.creditconnect.dto.UserResponse;
import com.creditconnect.exception.InvalidLoanStateException;
import com.creditconnect.repository.LoanRepository;
import com.creditconnect.service.LoanApplicationService;
import com.creditconnect.service.OfferService;
import com.creditconnect.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Sql("/test-lenders.sql")
class OfferAcceptanceConcurrencyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void onlyOneOfTwoSimultaneousAcceptsSucceeds() throws Exception {
        UserResponse user = userService.create(new CreateUserRequest(
                "Concurrent Customer", "concurrent@example.com", new BigDecimal("60000")));
        LoanApplicationResponse application = loanApplicationService.apply(
                new LoanApplicationRequest(user.id(), new BigDecimal("50000"), 12));
        List<LoanOfferResponse> offers =
                loanApplicationService.getOffers(application.applicationId());
        Long offerId = offers.getFirst().offerId();

        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        Queue<RuntimeException> failures = new ConcurrentLinkedQueue<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> first = pool.submit(() -> attemptAccept(startLine, offerId, succeeded, failures));
        Future<?> second = pool.submit(() -> attemptAccept(startLine, offerId, succeeded, failures));

        startLine.countDown();
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(succeeded.get()).isEqualTo(1);

        // The loser must be turned away by the status check that runs while the
        // application row is locked. If it fails on a constraint violation
        // instead, the lock is not doing its job and the database is the only
        // thing saving us.
        assertThat(failures).singleElement()
                .isInstanceOf(InvalidLoanStateException.class);

        assertThat(loanRepository.findByApplicationId(application.applicationId())).isPresent();
        assertThat(loanRepository.findAll())
                .filteredOn(loan -> loan.getApplication().getId()
                        .equals(application.applicationId()))
                .hasSize(1);
    }

    private void attemptAccept(CountDownLatch startLine, Long offerId,
                               AtomicInteger succeeded, Queue<RuntimeException> failures) {
        try {
            startLine.await();
            offerService.acceptOffer(offerId);
            succeeded.incrementAndGet();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ex) {
            failures.add(ex);
        }
    }
}
