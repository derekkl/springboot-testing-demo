package com.example.orderapi;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OrderStore {

    private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();

    public void save(Order order) {
        orders.put(order.id(), order);
    }

    public Order find(String id) {
        return orders.get(id);
    }
}
