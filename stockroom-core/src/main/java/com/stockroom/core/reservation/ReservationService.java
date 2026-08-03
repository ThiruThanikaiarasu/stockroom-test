package com.stockroom.core.reservation;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.stockroom.core.common.InsufficientStockException;
import com.stockroom.core.common.NotFoundException;
import com.stockroom.core.config.StockroomProperties;
import com.stockroom.core.event.DomainEventBus;
import com.stockroom.core.event.StockChangedEvent;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.sku.SkuService;
import com.stockroom.core.stock.StockLevel;
import com.stockroom.core.stock.StockLevelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservations;
    private final StockLevelRepository levels;
    private final SkuService skus;
    private final ReservationReferenceGenerator references;
    private final DomainEventBus events;
    private final Clock clock;
    private final Duration ttl;

    public ReservationService(ReservationRepository reservations, StockLevelRepository levels, SkuService skus,
                              ReservationReferenceGenerator references, DomainEventBus events, Clock clock,
                              StockroomProperties properties) {
        this.reservations = reservations;
        this.levels = levels;
        this.skus = skus;
        this.references = references;
        this.events = events;
        this.clock = clock;
        this.ttl = properties.reservations().ttl();
    }

    /**
     * Reserves {@code quantity} units of a SKU for an order from the first pickable bin that can
     * cover the whole quantity. Reservations are never split across bins.
     */
    public Reservation reserve(String orderRef, Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        Sku sku = skus.get(skuId);
        List<StockLevel> candidates = levels.findBySkuId(skuId).stream()
                .filter(level -> level.getBin().isPickable())
                .sorted(Comparator.comparing((StockLevel level) -> level.getBin().getCode()))
                .toList();
        StockLevel level = candidates.stream()
                .filter(candidate -> candidate.available() >= quantity)
                .findFirst()
                .orElseThrow(() -> new InsufficientStockException(sku.getCode(), quantity,
                        candidates.stream().mapToInt(StockLevel::available).max().orElse(0)));

        String reference = references.next();
        level.reserve(quantity);
        LocalDateTime now = LocalDateTime.now(clock);
        Reservation reservation = reservations.save(
                new Reservation(reference, orderRef, sku, level.getBin(), quantity, now, now.plus(ttl)));
        events.publish(new StockChangedEvent(skuId, sku.getCode(), "RESERVED", now));
        return reservation;
    }

    @Transactional(readOnly = true)
    public Reservation get(String reference) {
        return reservations.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Reservation", reference));
    }

    @Transactional(readOnly = true)
    public List<Reservation> activeForOrder(String orderRef) {
        return reservations.findByOrderRefAndStatus(orderRef, ReservationStatus.ACTIVE);
    }

    public Reservation release(String reference) {
        Reservation reservation = get(reference);
        reservation.release();
        returnToStock(reservation, "RELEASED");
        return reservation;
    }

    public Reservation extend(String reference) {
        Reservation reservation = get(reference);
        reservation.extendTo(LocalDateTime.now(clock).plus(ttl));
        return reservation;
    }

    /** Expires every active reservation whose expiry time has passed; returns how many expired. */
    public int expireDue() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> due = reservations.findActiveExpiringBefore(now);
        for (Reservation reservation : due) {
            reservation.expire();
            returnToStock(reservation, "EXPIRED");
        }
        if (!due.isEmpty()) {
            log.info("Expired {} reservation(s)", due.size());
        }
        return due.size();
    }

    private void returnToStock(Reservation reservation, String cause) {
        Long skuId = reservation.getSku().getId();
        levels.findBySkuIdAndBinId(skuId, reservation.getBin().getId())
                .ifPresent(level -> level.unreserve(reservation.getQuantity()));
        events.publish(new StockChangedEvent(skuId, reservation.getSku().getCode(), cause, LocalDateTime.now(clock)));
    }
}
