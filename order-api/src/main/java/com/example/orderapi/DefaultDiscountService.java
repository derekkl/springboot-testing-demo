package com.example.orderapi;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** 10% off orders of $100 or more; no discount otherwise. */
@Service
public class DefaultDiscountService implements DiscountService {

    private static final BigDecimal THRESHOLD = new BigDecimal("100");
    private static final BigDecimal RATE = new BigDecimal("0.10");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Override
    public BigDecimal getDiscountRate(BigDecimal subtotal) {
        return subtotal.compareTo(THRESHOLD) >= 0 ? RATE : ZERO;
    }
}
