package com.stockroom.api.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockroom.api.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ErrorResponseTest extends ApiTestSupport {

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mvc.perform(post("/api/skus").contentType(MediaType.APPLICATION_JSON).content("{\"code\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void nonNumericIdIsABadRequest() throws Exception {
        mvc.perform(get("/api/skus/{id}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void conflictCarriesTheDomainMessage() throws Exception {
        long sku = createSku(unique("ERR"), 0);
        receive(sku, createBin("A", 1), 1);

        postJson("/api/reservations", """
                {"orderRef": "SO-ERR", "skuId": %d, "quantity": 5}
                """.formatted(sku))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requested 5")));
    }
}
