package com.stockroom.api.alert;

import java.time.LocalDateTime;
import java.util.List;

import com.stockroom.core.alert.AlertService;
import com.stockroom.core.alert.LowStockAlert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alerts;

    public AlertController(AlertService alerts) {
        this.alerts = alerts;
    }

    @GetMapping
    public List<AlertResponse> open() {
        return alerts.open().stream().map(AlertResponse::from).toList();
    }

    @PostMapping("/{id}/acknowledge")
    public AlertResponse acknowledge(@PathVariable Long id) {
        return AlertResponse.from(alerts.acknowledge(id));
    }

    public record AlertResponse(Long id, String skuCode, int available, int reorderPoint, LocalDateTime raisedAt,
                                boolean acknowledged) {
        static AlertResponse from(LowStockAlert alert) {
            return new AlertResponse(alert.getId(), alert.getSku().getCode(), alert.getAvailable(),
                    alert.getReorderPoint(), alert.getRaisedAt(), alert.isAcknowledged());
        }
    }
}
