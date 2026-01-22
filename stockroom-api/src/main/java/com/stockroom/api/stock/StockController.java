package com.stockroom.api.stock;

import java.time.LocalDateTime;
import java.util.List;

import com.stockroom.core.stock.StockLevel;
import com.stockroom.core.stock.StockMovement;
import com.stockroom.core.stock.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skus/{skuId}/stock")
public class StockController {

    private final StockService stock;

    public StockController(StockService stock) {
        this.stock = stock;
    }

    @GetMapping
    public StockSummary summary(@PathVariable Long skuId) {
        List<BinStock> bins = stock.levels(skuId).stream().map(BinStock::from).toList();
        return new StockSummary(skuId, stock.onHand(skuId), stock.available(skuId), bins);
    }

    @GetMapping("/movements")
    public List<MovementResponse> movements(@PathVariable Long skuId) {
        return stock.history(skuId).stream().map(MovementResponse::from).toList();
    }

    @PostMapping("/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse receive(@PathVariable Long skuId, @Valid @RequestBody ReceiveRequest request) {
        return MovementResponse.from(stock.receive(skuId, request.binId(), request.quantity(), request.reference()));
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse adjust(@PathVariable Long skuId, @Valid @RequestBody AdjustRequest request) {
        return MovementResponse.from(stock.adjust(skuId, request.binId(), request.delta(), request.reason()));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MovementResponse> transfer(@PathVariable Long skuId, @Valid @RequestBody TransferRequest request) {
        return stock.transfer(skuId, request.fromBinId(), request.toBinId(), request.quantity(), request.reference())
                .stream().map(MovementResponse::from).toList();
    }

    public record ReceiveRequest(@NotNull Long binId, @Positive int quantity, String reference) {
    }

    public record AdjustRequest(@NotNull Long binId, int delta, @NotBlank String reason) {
    }

    public record TransferRequest(@NotNull Long fromBinId, @NotNull Long toBinId, @Positive int quantity,
                                  String reference) {
    }

    public record StockSummary(Long skuId, int onHand, int available, List<BinStock> bins) {
    }

    public record BinStock(Long binId, String binCode, int onHand, int reserved, int available) {
        static BinStock from(StockLevel level) {
            return new BinStock(level.getBin().getId(), level.getBin().getCode(), level.getOnHand(),
                    level.getReserved(), level.available());
        }
    }

    public record MovementResponse(Long id, String type, String binCode, int quantity, String reference,
                                   String transferId, LocalDateTime occurredAt) {
        static MovementResponse from(StockMovement movement) {
            return new MovementResponse(movement.getId(), movement.getType().name(), movement.getBin().getCode(),
                    movement.getQuantity(), movement.getReference(), movement.getTransferId(),
                    movement.getOccurredAt());
        }
    }
}
