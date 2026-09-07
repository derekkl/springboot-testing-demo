package com.example.orderapi;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderCalculator calculator;
    private final OrderStore store;

    public OrderController(OrderCalculator calculator, OrderStore store) {
        this.calculator = calculator;
        this.store = store;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> placeOrder(@RequestBody PlaceOrderRequest request) {
        try {
            String id = UUID.randomUUID().toString();
            Order order = calculator.calculate(id, request);
            store.save(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        Order order = store.find(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }
}
