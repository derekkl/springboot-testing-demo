package com.example.orderapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests: OrderCalculator is tested in complete isolation. Its one
 * dependency (DiscountService) is mocked via Mockito, so these tests never
 * start a Spring context, never touch HTTP, and run in milliseconds. This is
 * the fastest, cheapest layer -- it should have the most tests. Run via
 * `mvn test` (Surefire picks up *Tests.java by default).
 */
class OrderCalculatorTests {

    private static PlaceOrderRequest request(OrderItem... items) {
        return new PlaceOrderRequest("customer-1", List.of(items));
    }

    @Test
    void calculate_singleItem_computesCorrectSubtotal() {
        DiscountService discountService = mock(DiscountService.class);
        when(discountService.getDiscountRate(any())).thenReturn(BigDecimal.ZERO);
        OrderCalculator calculator = new OrderCalculator(discountService);

        Order order = calculator.calculate(
                "order-1", request(new OrderItem("WIDGET", 2, new BigDecimal("10.00"))));

        assertEquals(new BigDecimal("20.00"), order.subtotal());
        assertEquals(new BigDecimal("0.00"), order.discount());
        assertEquals(new BigDecimal("20.00"), order.total());
    }

    @Test
    void calculate_appliesDiscountFromDiscountService() {
        DiscountService discountService = mock(DiscountService.class);
        when(discountService.getDiscountRate(new BigDecimal("150.00")))
                .thenReturn(new BigDecimal("0.10"));
        OrderCalculator calculator = new OrderCalculator(discountService);

        Order order = calculator.calculate(
                "order-2", request(new OrderItem("WIDGET", 3, new BigDecimal("50.00"))));

        assertEquals(new BigDecimal("150.00"), order.subtotal());
        assertEquals(new BigDecimal("15.00"), order.discount());
        assertEquals(new BigDecimal("135.00"), order.total());
        // Confirms the calculator actually consulted the dependency rather
        // than hardcoding a rate -- only checkable because it's mocked.
        verify(discountService).getDiscountRate(new BigDecimal("150.00"));
    }

    @Test
    void calculate_emptyItems_throwsIllegalArgumentException() {
        OrderCalculator calculator = new OrderCalculator(mock(DiscountService.class));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate("order-3", request()));
    }

    @Test
    void calculate_zeroQuantity_throwsIllegalArgumentException() {
        OrderCalculator calculator = new OrderCalculator(mock(DiscountService.class));

        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate("order-4", request(new OrderItem("WIDGET", 0, new BigDecimal("10.00")))));
    }

    @Test
    void calculate_negativeUnitPrice_throwsIllegalArgumentException() {
        OrderCalculator calculator = new OrderCalculator(mock(DiscountService.class));

        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate("order-5", request(new OrderItem("WIDGET", 1, new BigDecimal("-5.00")))));
    }
}
