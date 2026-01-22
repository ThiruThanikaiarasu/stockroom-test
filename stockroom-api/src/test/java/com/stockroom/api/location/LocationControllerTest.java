package com.stockroom.api.location;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockroom.api.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LocationControllerTest extends ApiTestSupport {

    @Test
    void binsAreListedInAisleThenBayOrder() throws Exception {
        long location = body(postJson("/api/locations", """
                {"code": "%s", "name": "Pune DC"}
                """.formatted(unique("PNQ"))).andExpect(status().isCreated())).path("id").asLong();
        for (String bin : new String[] {"{\"aisle\": \"B\", \"bay\": 1}", "{\"aisle\": \"A\", \"bay\": 10}",
                "{\"aisle\": \"A\", \"bay\": 2}"}) {
            postJson("/api/locations/{id}/bins", bin, location).andExpect(status().isCreated());
        }

        mvc.perform(get("/api/locations/{id}/bins", location))
                .andExpect(jsonPath("$[*].code").value(org.hamcrest.Matchers.contains("A-2", "A-10", "B-1")));
    }

    @Test
    void invalidBinIsRejected() throws Exception {
        long location = body(postJson("/api/locations", """
                {"code": "%s", "name": "Pune DC"}
                """.formatted(unique("PNQ")))).path("id").asLong();

        postJson("/api/locations/{id}/bins", "{\"aisle\": \"A\", \"bay\": 0}", location)
                .andExpect(status().isBadRequest());
    }

    @Test
    void binCanBeMarkedNotPickable() throws Exception {
        long bin = createBin("Q", 9);

        mvc.perform(put("/api/locations/bins/{id}/pickable", bin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickable\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickable").value(false));
    }
}
