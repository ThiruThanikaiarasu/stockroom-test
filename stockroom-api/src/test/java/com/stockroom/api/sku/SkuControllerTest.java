package com.stockroom.api.sku;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockroom.api.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SkuControllerTest extends ApiTestSupport {

    @Test
    void createsAndReadsASku() throws Exception {
        String code = unique("mug");
        long id = createSku(code, 8);

        mvc.perform(get("/api/skus/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code.toUpperCase()))
                .andExpect(jsonPath("$.reorderPoint").value(8))
                .andExpect(jsonPath("$.averageCost").value(0))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void validatesNewSkus() throws Exception {
        postJson("/api/skus", """
                {"code": "", "name": "No code", "unitOfMeasure": "EA", "reorderPoint": -1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.length()").value(2));
    }

    @Test
    void duplicateCodeIsAConflict() throws Exception {
        String code = unique("DUP");
        createSku(code, 0);

        postJson("/api/skus", """
                {"code": "%s", "name": "Again", "unitOfMeasure": "EA", "reorderPoint": 0}
                """.formatted(code))
                .andExpect(status().isConflict());
    }

    @Test
    void patchUpdatesTheGivenFields() throws Exception {
        long id = createSku(unique("PAT"), 3);

        mvc.perform(patch("/api/skus/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Renamed\", \"description\": \"Now with lid\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.description").value("Now with lid"))
                .andExpect(jsonPath("$.reorderPoint").value(3));
    }

    @Test
    void patchWithNullDescriptionClearsIt() throws Exception {
        long id = createSku(unique("CLR"), 3);

        mvc.perform(patch("/api/skus/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void unknownSkuIs404() throws Exception {
        mvc.perform(get("/api/skus/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
