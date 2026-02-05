package com.stockroom.core.purchasing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import com.stockroom.core.common.NotFoundException;
import com.stockroom.core.costing.CostingService;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.sku.SkuService;
import com.stockroom.core.stock.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository orders;
    private final SkuService skus;
    private final StockService stock;
    private final CostingService costing;
    private final Clock clock;

    public PurchaseOrderService(PurchaseOrderRepository orders, SkuService skus, StockService stock,
                                CostingService costing, Clock clock) {
        this.orders = orders;
        this.skus = skus;
        this.stock = stock;
        this.costing = costing;
        this.clock = clock;
    }

    public PurchaseOrder create(NewPurchaseOrder request) {
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("a purchase order needs at least one line");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PurchaseOrder order = new PurchaseOrder(nextNumber(now), request.supplier(), now);
        for (NewPurchaseOrder.Line line : request.lines()) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("line quantity must be positive");
            }
            if (line.unitCost() == null || line.unitCost().signum() < 0) {
                throw new IllegalArgumentException("line unit cost must not be negative");
            }
            order.addLine(new PurchaseOrderLine(skus.get(line.skuId()), line.quantity(), line.unitCost()));
        }
        return orders.save(order);
    }

    @Transactional(readOnly = true)
    public PurchaseOrder get(Long id) {
        return orders.findWithLinesById(id).orElseThrow(() -> new NotFoundException("Purchase order", id));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> open() {
        return orders.findByStatusOrderByCreatedAtDesc(PurchaseOrderStatus.SUBMITTED);
    }

    public PurchaseOrder submit(Long id) {
        PurchaseOrder order = get(id);
        order.submit();
        return order;
    }

    public PurchaseOrder cancel(Long id) {
        PurchaseOrder order = get(id);
        order.cancel();
        return order;
    }

    public PurchaseOrder receive(Long id, List<ReceiptLine> receipt) {
        PurchaseOrder order = get(id);
        order.requireReceivable();
        for (ReceiptLine received : receipt) {
            if (received.quantity() <= 0) {
                throw new IllegalArgumentException("received quantity must be positive");
            }
            PurchaseOrderLine line = order.getLines().stream()
                    .filter(candidate -> candidate.getId().equals(received.lineId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Purchase order line", received.lineId()));
            Sku sku = line.getSku();
            BigDecimal cost = received.invoicedUnitCost() != null ? received.invoicedUnitCost() : line.getUnitCost();
            int onHandBefore = stock.onHand(sku.getId());
            line.receive(received.quantity());
            stock.receive(sku.getId(), received.binId(), received.quantity(), order.getNumber());
            costing.applyReceipt(sku, onHandBefore, received.quantity(), cost);
        }
        order.refreshStatus();
        return order;
    }

    private String nextNumber(LocalDateTime now) {
        String prefix = "PO-" + now.getYear() + "-";
        return prefix + String.format("%05d", orders.countByNumberStartingWith(prefix) + 1);
    }
}
