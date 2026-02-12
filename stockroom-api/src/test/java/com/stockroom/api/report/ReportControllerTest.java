package com.stockroom.api.report;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneOffset;

import com.stockroom.api.ApiTestSupport;
import org.junit.jupiter.api.Test;

class ReportControllerTest extends ApiTestSupport {

    @Test
    void throughputCoversRecentReceipts() throws Exception {
        String code = unique("RPT");
        long sku = createSku(code, 0);
        receive(sku, createBin("A", 1), 9);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        mvc.perform(get("/api/reports/throughput")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivedUnits").value(greaterThanOrEqualTo(9)))
                .andExpect(jsonPath("$.bySku[?(@.skuCode == '" + code + "')].unitsHandled").value(9));
    }

    @Test
    void valuationListsStockedSkus() throws Exception {
        String code = unique("VAL");
        long sku = createSku(code, 0);
        receive(sku, createBin("A", 2), 4);

        mvc.perform(get("/api/reports/valuation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[?(@.skuCode == '" + code + "')].onHand").value(4));
    }

    @Test
    void throughputNeedsBothDates() throws Exception {
        mvc.perform(get("/api/reports/throughput").param("from", "2026-01-01"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void reversedPeriodIsABadRequest() throws Exception {
        mvc.perform(get("/api/reports/throughput").param("from", "2026-02-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }
}
