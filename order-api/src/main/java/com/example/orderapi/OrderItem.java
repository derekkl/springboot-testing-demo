package com.example.orderapi;

import java.math.BigDecimal;

public record OrderItem(String sku, int quantity, BigDecimal unitPrice) {
}
