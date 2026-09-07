package com.example.orderapi.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Functional/E2E tests: exercise the real, separately-running app purely
 * over HTTP, the same way an actual client would. No shared code with the
 * app at all -- this class only knows the JSON wire contract, parsed with a
 * general-purpose library (Jackson), not the app's own model classes.
 */
class PlaceOrderJourneyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static ApiClient client;

    @BeforeAll
    static void startClient() throws Exception {
        client = ApiClient.start();
    }

    @AfterAll
    static void stopClient() {
        if (client != null) {
            client.stop();
        }
    }

    @Test
    void customerCanPlaceAnOrderAndRetrieveItAfterward() throws Exception {
        String body = """
                {"customerId":"e2e-customer","items":[{"sku":"WIDGET","quantity":4,"unitPrice":30.00}]}
                """;

        HttpResponse<String> placeResponse = client.post("/orders", body);
        assertEquals(201, placeResponse.statusCode());

        JsonNode placed = MAPPER.readTree(placeResponse.body());
        String orderId = placed.get("id").asText();
        double total = placed.get("total").asDouble();

        // $30 x 4 = $120, crosses the $100 discount threshold -> 10% off -> $108.
        assertEquals(108.00, total, 0.001);

        HttpResponse<String> getResponse = client.get("/orders/" + orderId);
        assertEquals(200, getResponse.statusCode());

        JsonNode fetched = MAPPER.readTree(getResponse.body());
        assertEquals(orderId, fetched.get("id").asText());
        assertEquals(total, fetched.get("total").asDouble(), 0.001);
    }

    @Test
    void rejectsAnOrderWithNoItems() throws Exception {
        String body = """
                {"customerId":"e2e-customer","items":[]}
                """;

        HttpResponse<String> response = client.post("/orders", body);

        assertEquals(400, response.statusCode());
    }
}
