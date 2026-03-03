package com.stockroom.core.alert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LowStockPolicyTest {

    private final LowStockPolicy policy = new LowStockPolicy();

    @Test
    void stockWellBelowReorderPointIsLow() {
        assertThat(policy.isLow(3, 10)).isTrue();
    }

    @Test
    void emptyStockIsLow() {
        assertThat(policy.isLow(0, 5)).isTrue();
    }

    @Test
    void plentifulStockIsNotLow() {
        assertThat(policy.isLow(40, 10)).isFalse();
    }

    @Test
    void skusWithoutReorderPointNeverAlert() {
        assertThat(policy.isLow(0, 0)).isFalse();
    }
}
