package com.stockroom.core.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.stockroom.core.common.InvalidStateException;
import com.stockroom.core.location.Bin;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.sku.SkuService;
import com.stockroom.core.stock.StockService;
import com.stockroom.testsupport.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PurchaseOrderServiceTest {

    @Autowired
    PurchaseOrderService orders;

    @Autowired
    StockService stock;

    @Autowired
    SkuService skus;

    @Autowired
    Fixtures fixtures;

    @Test
    void numbersOrdersPerYear() {
        Sku sku = fixtures.sku(0);

        PurchaseOrder first = orders.create(order(sku, 1, "1.00"));
        PurchaseOrder second = orders.create(order(sku, 1, "1.00"));

        assertThat(first.getNumber()).matches("PO-2026-\\d{5}");
        assertThat(second.getNumber()).isGreaterThan(first.getNumber());
        assertThat(first.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    @Test
    void onlySubmittedOrdersCanBeReceived() {
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.bin();
        PurchaseOrder po = orders.create(order(sku, 5, "2.00"));
        Long lineId = po.getLines().get(0).getId();

        assertThatThrownBy(() -> orders.receive(po.getId(), List.of(new ReceiptLine(lineId, bin.getId(), 5, null))))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void partialThenFullReceipt() {
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.bin();
        PurchaseOrder po = orders.create(order(sku, 10, "2.50"));
        orders.submit(po.getId());
        Long lineId = po.getLines().get(0).getId();

        PurchaseOrder partial = orders.receive(po.getId(), List.of(new ReceiptLine(lineId, bin.getId(), 4, null)));
        assertThat(partial.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);

        PurchaseOrder full = orders.receive(po.getId(), List.of(new ReceiptLine(lineId, bin.getId(), 6, null)));
        assertThat(full.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(stock.onHand(sku.getId())).isEqualTo(10);
    }

    @Test
    void cannotOverReceiveALine() {
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.bin();
        PurchaseOrder po = orders.create(order(sku, 3, "1.00"));
        orders.submit(po.getId());
        Long lineId = po.getLines().get(0).getId();

        assertThatThrownBy(() -> orders.receive(po.getId(), List.of(new ReceiptLine(lineId, bin.getId(), 4, null))))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("only 3 outstanding");
        assertThat(stock.onHand(sku.getId())).isZero();
    }

    @Test
    void cancelledOrdersCannotBeSubmitted() {
        Sku sku = fixtures.sku(0);
        PurchaseOrder po = orders.create(order(sku, 1, "1.00"));

        orders.cancel(po.getId());

        assertThatThrownBy(() -> orders.submit(po.getId())).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void ordersNeedLines() {
        assertThatThrownBy(() -> orders.create(new NewPurchaseOrder("Acme", List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static NewPurchaseOrder order(Sku sku, int quantity, String unitCost) {
        return new NewPurchaseOrder("Acme Supplies",
                List.of(new NewPurchaseOrder.Line(sku.getId(), quantity, new BigDecimal(unitCost))));
    }
}
