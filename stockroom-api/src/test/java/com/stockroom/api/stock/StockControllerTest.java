package com.stockroom.api.stock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockroom.api.ApiTestSupport;
import org.junit.jupiter.api.Test;

class StockControllerTest extends ApiTestSupport {

    @Test
    void receiptShowsUpInTheSummary() throws Exception {
        long sku = createSku(unique("STK"), 0);
        long bin = createBin("A", 3);

        receive(sku, bin, 17);

        mvc.perform(get("/api/skus/{id}/stock", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onHand").value(17))
                .andExpect(jsonPath("$.available").value(17))
                .andExpect(jsonPath("$.bins[0].binCode").value("A-3"));
    }

    @Test
    void transferReturnsBothLegs() throws Exception {
        long sku = createSku(unique("TRF"), 0);
        long from = createBin("A", 1);
        long to = createBin("B", 1);
        receive(sku, from, 10);

        postJson("/api/skus/{id}/stock/transfers", """
                {"fromBinId": %d, "toBinId": %d, "quantity": 4, "reference": "REPLEN"}
                """.formatted(from, to), sku)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$[1].type").value("TRANSFER_IN"));

        mvc.perform(get("/api/skus/{id}/stock/movements", sku))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void overdrawingAdjustmentIsAConflict() throws Exception {
        long sku = createSku(unique("ADJ"), 0);
        long bin = createBin("C", 2);
        receive(sku, bin, 2);

        postJson("/api/skus/{id}/stock/adjustments", """
                {"binId": %d, "delta": -5, "reason": "count"}
                """.formatted(bin), sku)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void adjustmentNeedsAReason() throws Exception {
        long sku = createSku(unique("ADJ"), 0);
        long bin = createBin("C", 3);

        postJson("/api/skus/{id}/stock/adjustments", """
                {"binId": %d, "delta": 1}
                """.formatted(bin), sku)
                .andExpect(status().isBadRequest());
    }
}
