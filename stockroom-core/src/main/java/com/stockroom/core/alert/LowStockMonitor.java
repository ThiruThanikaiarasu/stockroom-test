package com.stockroom.core.alert;

import java.time.Clock;
import java.time.LocalDateTime;

import com.stockroom.core.config.StockroomProperties;
import com.stockroom.core.event.DomainEventBus;
import com.stockroom.core.event.LowStockAlertRaised;
import com.stockroom.core.event.StockChangedEvent;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.sku.SkuRepository;
import com.stockroom.core.stock.StockLevelRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Raises at most one open alert per SKU: while an alert is unacknowledged, further drops do not
 * raise another one.
 */
@Component
public class LowStockMonitor {

    private static final Logger log = LoggerFactory.getLogger(LowStockMonitor.class);

    private final DomainEventBus events;
    private final LowStockPolicy policy;
    private final SkuRepository skus;
    private final StockLevelRepository levels;
    private final LowStockAlertRepository alerts;
    private final Clock clock;
    private final boolean enabled;
    private DomainEventBus.Subscription subscription;

    public LowStockMonitor(DomainEventBus events, LowStockPolicy policy, SkuRepository skus,
                           StockLevelRepository levels, LowStockAlertRepository alerts, Clock clock,
                           StockroomProperties properties) {
        this.events = events;
        this.policy = policy;
        this.skus = skus;
        this.levels = levels;
        this.alerts = alerts;
        this.clock = clock;
        this.enabled = properties.alerts().enabled();
    }

    @PostConstruct
    void start() {
        if (enabled) {
            subscription = events.subscribe(StockChangedEvent.class, this::onStockChanged);
        }
    }

    @PreDestroy
    void stop() {
        if (subscription != null) {
            subscription.cancel();
        }
    }

    void onStockChanged(StockChangedEvent event) {
        Sku sku = skus.findById(event.skuId()).orElse(null);
        if (sku == null || !sku.isActive()) {
            return;
        }
        int available = levels.availableForSku(sku.getId());
        if (!policy.isLow(available, sku.getReorderPoint())) {
            return;
        }
        if (alerts.existsBySkuIdAndAcknowledgedFalse(sku.getId())) {
            return;
        }
        LowStockAlert alert = alerts.save(
                new LowStockAlert(sku, available, sku.getReorderPoint(), LocalDateTime.now(clock)));
        log.info("Low stock on {}: {} available, reorder point {}", sku.getCode(), available, sku.getReorderPoint());
        events.publish(new LowStockAlertRaised(alert.getId(), sku.getId(), sku.getCode(), available,
                sku.getReorderPoint()));
    }
}
