package com.example.orderapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Integration tests: a real embedded Tomcat instance and the real Spring
 * context -- routing, JSON (de)serialization, dependency injection, the
 * *real* DefaultDiscountService bean -- all wired together exactly as in
 * production. It's still in-process, though: no separate OS process, no
 * `java -jar`. This layer catches wiring bugs pure unit tests can't see (a
 * misspelled mapping, a bean that isn't registered), while still running
 * fast enough for constant use.
 *
 * Named *IT.java (not *Test.java) so Surefire skips it during `mvn test`
 * and Failsafe picks it up during `mvn verify`, after the app is packaged.
 *
 * @AutoConfigureTestRestTemplate is required as of Spring Boot 4 --
 * TestRestTemplate is no longer auto-configured into @SpringBootTest by
 * default the way it was in Spring Boot 3.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderEndpointsIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void postOrders_validRequest_returns201WithComputedTotal() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                "customer-1", List.of(new OrderItem("WIDGET", 2, new BigDecimal("10.00"))));

        ResponseEntity<Order> response = restTemplate.postForEntity(url("/orders"), request, Order.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().total()).isEqualByComparingTo("20.00");
    }

    @Test
    void postOrders_emptyItems_returns400() {
        PlaceOrderRequest request = new PlaceOrderRequest("customer-1", List.of());

        ResponseEntity<String> response = restTemplate.postForEntity(url("/orders"), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getOrder_afterPlacing_returnsMatchingOrder() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                "customer-2", List.of(new OrderItem("GADGET", 1, new BigDecimal("25.00"))));
        Order created = restTemplate.postForEntity(url("/orders"), request, Order.class).getBody();

        ResponseEntity<Order> response = restTemplate.getForEntity(url("/orders/" + created.id()), Order.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(created.id());
        assertThat(response.getBody().total()).isEqualByComparingTo(created.total());
    }

    @Test
    void getOrder_unknownId_returns404() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(url("/orders/" + java.util.UUID.randomUUID()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
