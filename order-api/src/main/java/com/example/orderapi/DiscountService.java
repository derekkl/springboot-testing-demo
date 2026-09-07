package com.example.orderapi;

import java.math.BigDecimal;

public interface DiscountService {

    /** Returns the discount rate (e.g. 0.10 for 10%) to apply to the given subtotal. */
    BigDecimal getDiscountRate(BigDecimal subtotal);
}
