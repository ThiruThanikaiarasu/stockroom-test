package com.stockroom.api.reservation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockroom.api.ApiTestSupport;
import org.junit.jupiter.api.Test;

class ReservationControllerTest extends ApiTestSupport {

    @Test
    void reserveReadAndRelease() throws Exception {
        long sku = createSku(unique("RES"), 0);
        receive(sku, createBin("A", 1), 6);
        String order = unique("SO");

        String reference = body(postJson("/api/reservations", """
                {"orderRef": "%s", "skuId": %d, "quantity": 2}
                """.formatted(order, sku))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.binCode").value("A-1"))).path("reference").asText();

        mvc.perform(get("/api/reservations").param("orderRef", order))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(post("/api/reservations/{ref}/release", reference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        mvc.perform(get("/api/skus/{id}/stock", sku)).andExpect(jsonPath("$.available").value(6));
    }

    @Test
    void reservingMoreThanAvailableIsAConflict() throws Exception {
        long sku = createSku(unique("RES"), 0);
        receive(sku, createBin("A", 1), 1);

        postJson("/api/reservations", """
                {"orderRef": "SO-1", "skuId": %d, "quantity": 2}
                """.formatted(sku))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void unknownReservationIs404() throws Exception {
        mvc.perform(get("/api/reservations/{ref}", "RSV-NOPE")).andExpect(status().isNotFound());
    }
}
