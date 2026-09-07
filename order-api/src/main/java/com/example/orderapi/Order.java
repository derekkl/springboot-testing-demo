package com.example.orderapi;

import java.math.BigDecimal;
import java.util.List;

public record Order(
        String id,
        String customerId,
        List<OrderItem> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {
}
