package com.stockroom.api.purchasing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockroom.api.ApiTestSupport;
import org.junit.jupiter.api.Test;

class PurchaseOrderControllerTest extends ApiTestSupport {

    @Test
    void createSubmitAndReceive() throws Exception {
        long sku = createSku(unique("PO"), 0);
        long bin = createBin("R", 1);

        JsonNode po = body(postJson("/api/purchase-orders", """
                {"supplier": "Globex", "lines": [{"skuId": %d, "quantity": 20, "unitCost": 3.75}]}
                """.formatted(sku))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT")));
        long id = po.path("id").asLong();
        long line = po.path("lines").get(0).path("id").asLong();

        mvc.perform(post("/api/purchase-orders/{id}/submit", id))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        postJson("/api/purchase-orders/{id}/receipts", """
                {"lines": [{"lineId": %d, "binId": %d, "quantity": 20}]}
                """.formatted(line, bin), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.lines[0].received").value(20));
    }

    @Test
    void emptyOrdersAreRejected() throws Exception {
        postJson("/api/purchase-orders", """
                {"supplier": "Globex", "lines": []}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void receivingADraftIsAConflict() throws Exception {
        long sku = createSku(unique("PO"), 0);
        long bin = createBin("R", 2);
        JsonNode po = body(postJson("/api/purchase-orders", """
                {"supplier": "Globex", "lines": [{"skuId": %d, "quantity": 1, "unitCost": 1}]}
                """.formatted(sku)));

        postJson("/api/purchase-orders/{id}/receipts", """
                {"lines": [{"lineId": %d, "binId": %d, "quantity": 1}]}
                """.formatted(po.path("lines").get(0).path("id").asLong(), bin), po.path("id").asLong())
                .andExpect(status().isConflict());
    }
}
