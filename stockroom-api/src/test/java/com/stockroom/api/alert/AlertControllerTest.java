package com.stockroom.api.alert;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockroom.api.ApiTestSupport;
import org.junit.jupiter.api.Test;

class AlertControllerTest extends ApiTestSupport {

    @Test
    void lowStockAlertCanBeAcknowledged() throws Exception {
        String code = unique("ALR");
        long sku = createSku(code, 50);
        receive(sku, createBin("A", 1), 5);

        JsonNode open = body(mvc.perform(get("/api/alerts")).andExpect(status().isOk()));
        long alertId = -1;
        for (JsonNode alert : open) {
            if (alert.path("skuCode").asText().equals(code)) {
                alertId = alert.path("id").asLong();
            }
        }

        mvc.perform(post("/api/alerts/{id}/acknowledge", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(true))
                .andExpect(jsonPath("$.available").value(5));
    }

    @Test
    void unknownAlertIs404() throws Exception {
        mvc.perform(post("/api/alerts/{id}/acknowledge", 424242)).andExpect(status().isNotFound());
    }
}
