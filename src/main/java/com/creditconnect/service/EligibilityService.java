package com.creditconnect.service;

import com.creditconnect.model.Lender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Holds the lending rules. Deliberately has no repository dependencies so the
 * rules can be tested on their own.
 */
@Service
public class EligibilityService {

    private static final BigDecimal HIGH_INCOME_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal HIGH_INCOME_MULTIPLIER = new BigDecimal("5");
    private static final BigDecimal LOW_INCOME_MULTIPLIER = new BigDecimal("2");
    private static final BigDecimal LEVERAGE_THRESHOLD_MULTIPLIER = new BigDecimal("3");
    private static final BigDecimal LEVERAGE_SURCHARGE = new BigDecimal("1.50");
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");
    private static final BigDecimal PERCENT = new BigDecimal("100");

    public BigDecimal maxEligibleAmount(BigDecimal monthlyIncome) {
        BigDecimal multiplier = monthlyIncome.compareTo(HIGH_INCOME_THRESHOLD) >= 0
                ? HIGH_INCOME_MULTIPLIER
                : LOW_INCOME_MULTIPLIER;
        return monthlyIncome.multiply(multiplier);
    }

    public boolean canLenderServe(Lender lender, BigDecimal monthlyIncome,
                                  BigDecimal amount, int tenureMonths) {
        return lender.isActive()
                && lender.getAvailableAmount().compareTo(amount) >= 0
                && monthlyIncome.compareTo(lender.getMinIncome()) >= 0
                && tenureMonths <= lender.getMaxTenureMonths();
    }

    public BigDecimal priceOffer(Lender lender, BigDecimal monthlyIncome, BigDecimal amount) {
        BigDecimal leverageThreshold = monthlyIncome.multiply(LEVERAGE_THRESHOLD_MULTIPLIER);
        if (amount.compareTo(leverageThreshold) > 0) {
            return lender.getBaseInterestRate().add(LEVERAGE_SURCHARGE);
        }
        return lender.getBaseInterestRate();
    }

    /**
     * Reducing balance EMI: P * r * (1+r)^n / ((1+r)^n - 1), where r is the
     * monthly rate. A zero rate would divide by zero, so it is handled separately.
     */
    public BigDecimal monthlyInstalment(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (annualRate.signum() == 0) {
            return principal.divide(new BigDecimal(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRate
                .divide(MONTHS_PER_YEAR, MathContext.DECIMAL64)
                .divide(PERCENT, MathContext.DECIMAL64);
        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(tenureMonths, MathContext.DECIMAL64);

        return principal.multiply(monthlyRate).multiply(growth)
                .divide(growth.subtract(BigDecimal.ONE), MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
