package com.stockroom.api.purchasing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.stockroom.core.purchasing.NewPurchaseOrder;
import com.stockroom.core.purchasing.PurchaseOrder;
import com.stockroom.core.purchasing.PurchaseOrderLine;
import com.stockroom.core.purchasing.PurchaseOrderService;
import com.stockroom.core.purchasing.ReceiptLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService orders;

    public PurchaseOrderController(PurchaseOrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        List<NewPurchaseOrder.Line> lines = request.lines().stream()
                .map(line -> new NewPurchaseOrder.Line(line.skuId(), line.quantity(), line.unitCost()))
                .toList();
        return PurchaseOrderResponse.from(orders.create(new NewPurchaseOrder(request.supplier(), lines)));
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse get(@PathVariable Long id) {
        return PurchaseOrderResponse.from(orders.get(id));
    }

    @GetMapping
    public List<PurchaseOrderResponse> open() {
        return orders.open().stream().map(PurchaseOrderResponse::from).toList();
    }

    @PostMapping("/{id}/submit")
    public PurchaseOrderResponse submit(@PathVariable Long id) {
        return PurchaseOrderResponse.from(orders.submit(id));
    }

    @PostMapping("/{id}/cancel")
    public PurchaseOrderResponse cancel(@PathVariable Long id) {
        return PurchaseOrderResponse.from(orders.cancel(id));
    }

    @PostMapping("/{id}/receipts")
    public PurchaseOrderResponse receive(@PathVariable Long id, @Valid @RequestBody ReceiveRequest request) {
        List<ReceiptLine> lines = request.lines().stream()
                .map(line -> new ReceiptLine(line.lineId(), line.binId(), line.quantity(), line.invoicedUnitCost()))
                .toList();
        return PurchaseOrderResponse.from(orders.receive(id, lines));
    }

    public record CreatePurchaseOrderRequest(@NotBlank String supplier, @NotEmpty List<@Valid LineRequest> lines) {
    }

    public record LineRequest(@NotNull Long skuId, @Positive int quantity, @NotNull @PositiveOrZero BigDecimal unitCost) {
    }

    public record ReceiveRequest(@NotEmpty List<@Valid ReceiveLineRequest> lines) {
    }

    public record ReceiveLineRequest(@NotNull Long lineId, @NotNull Long binId, @Positive int quantity,
                                     @PositiveOrZero BigDecimal invoicedUnitCost) {
    }

    public record PurchaseOrderResponse(Long id, String number, String supplier, String status,
                                        LocalDateTime createdAt, List<LineResponse> lines) {
        static PurchaseOrderResponse from(PurchaseOrder order) {
            return new PurchaseOrderResponse(order.getId(), order.getNumber(), order.getSupplier(),
                    order.getStatus().name(), order.getCreatedAt(),
                    order.getLines().stream().map(LineResponse::from).toList());
        }
    }

    public record LineResponse(Long id, String skuCode, int ordered, int received, BigDecimal unitCost) {
        static LineResponse from(PurchaseOrderLine line) {
            return new LineResponse(line.getId(), line.getSku().getCode(), line.getOrderedQuantity(),
                    line.getReceivedQuantity(), line.getUnitCost());
        }
    }
}
