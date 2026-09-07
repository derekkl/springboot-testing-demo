package com.example.orderapi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class OrderCalculator {

    private final DiscountService discountService;

    public OrderCalculator(DiscountService discountService) {
        this.discountService = discountService;
    }

    public Order calculate(String id, PlaceOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        for (OrderItem item : request.items()) {
            if (item.quantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity for " + item.sku() + " must be greater than zero.");
            }
            if (item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Unit price for " + item.sku() + " cannot be negative.");
            }
        }

        BigDecimal subtotal = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountRate = discountService.getDiscountRate(subtotal);
        BigDecimal discount = subtotal.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discount);

        return new Order(id, request.customerId(), request.items(), subtotal, discount, total);
    }
}
