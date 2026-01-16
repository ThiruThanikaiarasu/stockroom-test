package com.stockroom.core.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.stockroom.core.common.InsufficientStockException;
import com.stockroom.core.location.Bin;
import com.stockroom.core.location.Location;
import com.stockroom.core.reservation.ReservationService;
import com.stockroom.core.sku.Sku;
import com.stockroom.testsupport.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StockServiceTest {

    @Autowired
    StockService stock;

    @Autowired
    ReservationService reservations;

    @Autowired
    Fixtures fixtures;

    @Test
    void receiptIncreasesOnHandAndIsRecorded() {
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.bin();

        StockMovement movement = stock.receive(sku.getId(), bin.getId(), 24, "PO-1");

        assertThat(movement.getType()).isEqualTo(MovementType.RECEIPT);
        assertThat(movement.getQuantity()).isEqualTo(24);
        assertThat(stock.onHand(sku.getId())).isEqualTo(24);
        assertThat(stock.available(sku.getId())).isEqualTo(24);
    }

    @Test
    void negativeAdjustmentCannotTouchReservedStock() {
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.stocked(sku, 10);
        reservations.reserve(Fixtures.unique("ORD"), sku.getId(), 8);

        assertThatThrownBy(() -> stock.adjust(sku.getId(), bin.getId(), -3, "count"))
                .isInstanceOf(InsufficientStockException.class);

        stock.adjust(sku.getId(), bin.getId(), -2, "damaged");
        assertThat(stock.onHand(sku.getId())).isEqualTo(8);
        assertThat(stock.available(sku.getId())).isZero();
    }

    @Test
    void zeroAdjustmentIsRejected() {
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.stocked(sku, 1);

        assertThatThrownBy(() -> stock.adjust(sku.getId(), bin.getId(), 0, "noop"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferMovesStockAndWritesALegPerBin() {
        Sku sku = fixtures.sku(0);
        Location location = fixtures.location();
        Bin from = fixtures.bin(location, "A", 1);
        Bin to = fixtures.bin(location, "D", 4);
        stock.receive(sku.getId(), from.getId(), 30, "PO-2");

        List<StockMovement> legs = stock.transfer(sku.getId(), from.getId(), to.getId(), 12, "REPLEN-1");

        assertThat(legs).extracting(StockMovement::getType)
                .containsExactly(MovementType.TRANSFER_OUT, MovementType.TRANSFER_IN);
        assertThat(legs).extracting(StockMovement::getQuantity).containsExactly(-12, 12);
        assertThat(legs.get(0).getTransferId()).isNotNull().isEqualTo(legs.get(1).getTransferId());
        assertThat(stock.levels(sku.getId())).extracting(StockLevel::getOnHand).containsExactly(18, 12);
        assertThat(stock.onHand(sku.getId())).isEqualTo(30);
    }

    @Test
    void transferNeedsUnreservedStock() {
        Sku sku = fixtures.sku(0);
        Location location = fixtures.location();
        Bin from = fixtures.bin(location, "A", 1);
        Bin to = fixtures.bin(location, "A", 2);
        stock.receive(sku.getId(), from.getId(), 5, "PO-3");
        reservations.reserve(Fixtures.unique("ORD"), sku.getId(), 4);

        assertThatThrownBy(() -> stock.transfer(sku.getId(), from.getId(), to.getId(), 2, "REPLEN"))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void pickingConsumesReservedStock() {
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.stocked(sku, 10);
        reservations.reserve(Fixtures.unique("ORD"), sku.getId(), 4);

        stock.pickReserved(sku.getId(), bin.getId(), 4, "ORD");

        assertThat(stock.onHand(sku.getId())).isEqualTo(6);
        assertThat(stock.available(sku.getId())).isEqualTo(6);
        assertThat(stock.history(sku.getId())).extracting(StockMovement::getType)
                .containsExactly(MovementType.RECEIPT, MovementType.PICK);
    }

    @Test
    void receiptQuantityMustBePositive() {
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.bin();

        assertThatThrownBy(() -> stock.receive(sku.getId(), bin.getId(), 0, "PO"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
