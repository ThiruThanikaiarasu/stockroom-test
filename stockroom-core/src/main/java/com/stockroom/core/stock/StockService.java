package com.stockroom.core.stock;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.stockroom.core.common.InsufficientStockException;
import com.stockroom.core.event.DomainEventBus;
import com.stockroom.core.event.StockChangedEvent;
import com.stockroom.core.location.Bin;
import com.stockroom.core.location.LocationService;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.sku.SkuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockService {

    private final StockLevelRepository levels;
    private final StockMovementRepository movements;
    private final SkuService skus;
    private final LocationService locations;
    private final DomainEventBus events;
    private final Clock clock;

    public StockService(StockLevelRepository levels, StockMovementRepository movements, SkuService skus,
                        LocationService locations, DomainEventBus events, Clock clock) {
        this.levels = levels;
        this.movements = movements;
        this.skus = skus;
        this.locations = locations;
        this.events = events;
        this.clock = clock;
    }

    public StockMovement receive(Long skuId, Long binId, int quantity, String reference) {
        requirePositive(quantity);
        StockLevel level = levelFor(skuId, binId);
        level.add(quantity);
        return record(level, MovementType.RECEIPT, quantity, reference, null);
    }

    /** Applies a signed stock-count correction. Reserved stock can never be adjusted away. */
    public StockMovement adjust(Long skuId, Long binId, int delta, String reason) {
        if (delta == 0) {
            throw new IllegalArgumentException("adjustment must not be zero");
        }
        StockLevel level = levelFor(skuId, binId);
        if (delta > 0) {
            level.add(delta);
        } else {
            requireAvailable(level, -delta);
            level.remove(-delta);
        }
        return record(level, MovementType.ADJUSTMENT, delta, reason, null);
    }

    /** Moves unreserved stock between two bins; writes one movement per bin. */
    public List<StockMovement> transfer(Long skuId, Long fromBinId, Long toBinId, int quantity, String reference) {
        requirePositive(quantity);
        if (fromBinId.equals(toBinId)) {
            throw new IllegalArgumentException("source and destination bins must differ");
        }
        StockLevel from = levelFor(skuId, fromBinId);
        requireAvailable(from, quantity);
        StockLevel to = levelFor(skuId, toBinId);
        String transferId = UUID.randomUUID().toString();
        from.remove(quantity);
        to.add(quantity);
        StockMovement out = record(from, MovementType.TRANSFER_OUT, -quantity, reference, transferId);
        StockMovement in = record(to, MovementType.TRANSFER_IN, quantity, reference, transferId);
        return List.of(out, in);
    }

    /** Ships reserved stock out of a bin. */
    public StockMovement pickReserved(Long skuId, Long binId, int quantity, String reference) {
        requirePositive(quantity);
        StockLevel level = levels.findBySkuIdAndBinId(skuId, binId)
                .orElseThrow(() -> new IllegalStateException("no stock level for sku " + skuId + " in bin " + binId));
        if (quantity > level.getReserved()) {
            throw new IllegalStateException("cannot pick more than the reserved quantity");
        }
        level.consumeReserved(quantity);
        return record(level, MovementType.PICK, -quantity, reference, null);
    }

    @Transactional(readOnly = true)
    public int available(Long skuId) {
        return levels.availableForSku(skuId);
    }

    @Transactional(readOnly = true)
    public int onHand(Long skuId) {
        return levels.onHandForSku(skuId);
    }

    @Transactional(readOnly = true)
    public List<StockLevel> levels(Long skuId) {
        skus.get(skuId);
        return levels.findBySkuId(skuId);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> history(Long skuId) {
        return movements.findBySkuIdOrderByOccurredAtAscIdAsc(skuId);
    }

    StockLevel levelFor(Long skuId, Long binId) {
        return levels.findBySkuIdAndBinId(skuId, binId).orElseGet(() -> {
            Sku sku = skus.get(skuId);
            Bin bin = locations.getBin(binId);
            return levels.save(new StockLevel(sku, bin));
        });
    }

    private StockMovement record(StockLevel level, MovementType type, int quantity, String reference,
                                 String transferId) {
        LocalDateTime now = LocalDateTime.now(clock);
        StockMovement movement = movements.save(
                new StockMovement(level.getSku(), level.getBin(), type, quantity, reference, transferId, now));
        events.publish(new StockChangedEvent(level.getSku().getId(), level.getSku().getCode(), type.name(), now));
        return movement;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    private static void requireAvailable(StockLevel level, int quantity) {
        if (level.available() < quantity) {
            throw new InsufficientStockException(level.getSku().getCode(), quantity, level.available());
        }
    }
}
