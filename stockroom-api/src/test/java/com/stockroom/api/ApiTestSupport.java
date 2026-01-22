package com.stockroom.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestSupport {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected static String unique(String prefix) {
        return prefix + "-" + SEQUENCE.incrementAndGet();
    }

    protected ResultActions postJson(String uri, String body, Object... vars) throws Exception {
        return mvc.perform(post(uri, vars).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected long createSku(String code, int reorderPoint) throws Exception {
        String request = """
                {"code": "%s", "name": "Item %s", "description": "Created by test", "unitOfMeasure": "EA",
                 "reorderPoint": %d}
                """.formatted(code, code, reorderPoint);
        return body(postJson("/api/skus", request).andExpect(status().isCreated())).path("id").asLong();
    }

    protected long createBin(String aisle, int bay) throws Exception {
        long location = body(postJson("/api/locations", """
                {"code": "%s", "name": "Test DC"}
                """.formatted(unique("LOC"))).andExpect(status().isCreated())).path("id").asLong();
        return body(postJson("/api/locations/{id}/bins", """
                {"aisle": "%s", "bay": %d}
                """.formatted(aisle, bay), location).andExpect(status().isCreated())).path("id").asLong();
    }

    protected void receive(long skuId, long binId, int quantity) throws Exception {
        postJson("/api/skus/{id}/stock/receipts", """
                {"binId": %d, "quantity": %d, "reference": "OPENING"}
                """.formatted(binId, quantity), skuId).andExpect(status().isCreated());
    }
}
