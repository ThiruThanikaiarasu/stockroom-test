package com.stockroom.core.alert;

import java.util.List;

import com.stockroom.core.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AlertService {

    private final LowStockAlertRepository alerts;

    public AlertService(LowStockAlertRepository alerts) {
        this.alerts = alerts;
    }

    @Transactional(readOnly = true)
    public List<LowStockAlert> open() {
        return alerts.findOpen();
    }

    public LowStockAlert acknowledge(Long id) {
        LowStockAlert alert = alerts.findById(id).orElseThrow(() -> new NotFoundException("Alert", id));
        alert.acknowledge();
        return alert;
    }
}
