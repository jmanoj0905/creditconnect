package com.creditconnect.service;

import com.creditconnect.model.Lender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityServiceTest {

    private EligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        eligibilityService = new EligibilityService();
    }

    @Test
    void appliesFiveTimesMultiplierAtTheIncomeThreshold() {
        BigDecimal limit = eligibilityService.maxEligibleAmount(new BigDecimal("50000"));

        assertThat(limit).isEqualByComparingTo("250000");
    }

    @Test
    void appliesTwoTimesMultiplierBelowTheIncomeThreshold() {
        BigDecimal limit = eligibilityService.maxEligibleAmount(new BigDecimal("49999"));

        assertThat(limit).isEqualByComparingTo("99998");
    }

    @Test
    void lenderCanServeWhenAllCriteriaMatch() {
        Lender lender = lenderWith("5000000", "40000", "12.00", 36);

        boolean result = eligibilityService.canLenderServe(
                lender, new BigDecimal("60000"), new BigDecimal("50000"), 12);

        assertThat(result).isTrue();
    }

    @Test
    void lenderCannotServeWhenPoolIsTooSmall() {
        Lender lender = lenderWith("40000", "40000", "12.00", 36);

        boolean result = eligibilityService.canLenderServe(
                lender, new BigDecimal("60000"), new BigDecimal("50000"), 12);

        assertThat(result).isFalse();
    }

    @Test
    void lenderCannotServeWhenIncomeIsBelowItsFloor() {
        Lender lender = lenderWith("5000000", "70000", "12.00", 36);

        boolean result = eligibilityService.canLenderServe(
                lender, new BigDecimal("60000"), new BigDecimal("50000"), 12);

        assertThat(result).isFalse();
    }

    @Test
    void lenderCannotServeWhenTenureExceedsItsMaximum() {
        Lender lender = lenderWith("5000000", "40000", "12.00", 12);

        boolean result = eligibilityService.canLenderServe(
                lender, new BigDecimal("60000"), new BigDecimal("50000"), 24);

        assertThat(result).isFalse();
    }

    @Test
    void pricesAtBaseRateWhenLeverageIsModest() {
        Lender lender = lenderWith("5000000", "40000", "12.00", 36);

        BigDecimal rate = eligibilityService.priceOffer(
                lender, new BigDecimal("60000"), new BigDecimal("50000"));

        assertThat(rate).isEqualByComparingTo("12.00");
    }

    @Test
    void addsSurchargeWhenAmountExceedsThreeTimesIncome() {
        Lender lender = lenderWith("5000000", "40000", "12.00", 36);

        BigDecimal rate = eligibilityService.priceOffer(
                lender, new BigDecimal("60000"), new BigDecimal("200000"));

        assertThat(rate).isEqualByComparingTo("13.50");
    }

    @Test
    void computesMonthlyInstalment() {
        BigDecimal emi = eligibilityService.monthlyInstalment(
                new BigDecimal("50000"), new BigDecimal("12.00"), 12);

        assertThat(emi).isEqualByComparingTo("4442.44");
    }

    @Test
    void treatsZeroInterestAsPlainDivision() {
        BigDecimal emi = eligibilityService.monthlyInstalment(
                new BigDecimal("12000"), BigDecimal.ZERO, 12);

        assertThat(emi).isEqualByComparingTo("1000.00");
    }

    private Lender lenderWith(String pool, String minIncome, String rate, int maxTenure) {
        return new Lender("Test Lender", new BigDecimal(pool), new BigDecimal(minIncome),
                new BigDecimal(rate), maxTenure);
    }
}
