package com.example.orderapi;

import java.util.List;

public record PlaceOrderRequest(String customerId, List<OrderItem> items) {
}
