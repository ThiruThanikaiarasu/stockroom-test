package com.stockroom.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.stockroom.core.location.Bin;
import com.stockroom.core.purchasing.NewPurchaseOrder;
import com.stockroom.core.purchasing.PurchaseOrder;
import com.stockroom.core.purchasing.PurchaseOrderService;
import com.stockroom.core.purchasing.ReceiptLine;
import com.stockroom.core.sku.Sku;
import com.stockroom.testsupport.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ValuationReportServiceTest {

    @Autowired
    ValuationReportService valuation;

    @Autowired
    RevaluationReportService revaluations;

    @Autowired
    PurchaseOrderService orders;

    @Autowired
    Fixtures fixtures;

    @Test
    void valuesStockAtAverageCost() {
        Sku sku = fixtures.sku(0);
        receive(sku, 12, "2.5000");

        ValuationReport report = valuation.valuation();

        assertThat(report.lines()).filteredOn(line -> line.skuCode().equals(sku.getCode()))
                .singleElement()
                .satisfies(line -> {
                    assertThat(line.onHand()).isEqualTo(12);
                    assertThat(line.value()).isEqualByComparingTo("30.00");
                });
        assertThat(report.totalValue()).isGreaterThanOrEqualTo(new BigDecimal("30.00"));
    }

    @Test
    void skusWithoutStockAreLeftOut() {
        Sku sku = fixtures.sku(0);

        assertThat(valuation.valuation().lines()).extracting(ValuationReport.Line::skuCode)
                .doesNotContain(sku.getCode());
    }

    @Test
    void revaluationHistoryFollowsReceipts() {
        Sku sku = fixtures.sku(0);
        receive(sku, 10, "4.0000");
        receive(sku, 10, "6.0000");

        RevaluationReport report = revaluations.forSku(sku.getCode());

        assertThat(report.entries()).extracting(RevaluationReport.Entry::newCost)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("4"), new BigDecimal("5"));
    }

    private void receive(Sku sku, int quantity, String cost) {
        Bin bin = fixtures.bin();
        PurchaseOrder po = orders.create(new NewPurchaseOrder("Acme",
                List.of(new NewPurchaseOrder.Line(sku.getId(), quantity, new BigDecimal(cost)))));
        orders.submit(po.getId());
        orders.receive(po.getId(), List.of(new ReceiptLine(po.getLines().get(0).getId(), bin.getId(), quantity, null)));
    }
}
