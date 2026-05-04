package com.stockroom.api.picking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockroom.api.ApiTestSupport;
import org.junit.jupiter.api.Test;

class PickListControllerTest extends ApiTestSupport {

    @Test
    void createAndCompleteAPickList() throws Exception {
        long sku = createSku(unique("PCK"), 0);
        receive(sku, createBin("F", 2), 5);
        String order = unique("SO");
        postJson("/api/reservations", """
                {"orderRef": "%s", "skuId": %d, "quantity": 3}
                """.formatted(order, sku)).andExpect(status().isCreated());

        String id = body(postJson("/api/pick-lists", """
                {"orderRef": "%s"}
                """.formatted(order))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].binCode").value("F-2"))).path("id").asText();

        mvc.perform(get("/api/pick-lists/summary")).andExpect(jsonPath("$['" + id + "']").value(1));

        mvc.perform(post("/api/pick-lists/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.lines[0].picked").value(3));

        mvc.perform(get("/api/skus/{id}/stock", sku)).andExpect(jsonPath("$.onHand").value(2));
    }

    @Test
    void orderWithoutReservationsIsAConflict() throws Exception {
        postJson("/api/pick-lists", """
                {"orderRef": "SO-NONE"}
                """).andExpect(status().isConflict());
    }

    @Test
    void malformedPickListIdIsABadRequest() throws Exception {
        mvc.perform(get("/api/pick-lists/{id}", "not-a-uuid")).andExpect(status().isBadRequest());
    }
}
