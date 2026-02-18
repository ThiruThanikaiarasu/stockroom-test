package com.stockroom.core.picking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockroom.core.common.InvalidStateException;
import com.stockroom.core.location.Location;
import com.stockroom.core.reservation.Reservation;
import com.stockroom.core.reservation.ReservationService;
import com.stockroom.core.reservation.ReservationStatus;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.stock.StockService;
import com.stockroom.testsupport.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PickListServiceTest {

    @Autowired
    PickListService pickLists;

    @Autowired
    ReservationService reservations;

    @Autowired
    StockService stock;

    @Autowired
    Fixtures fixtures;

    @Test
    void ordersLinesByBinCode() {
        Location location = fixtures.location();
        String order = Fixtures.unique("ORD");
        reserveIn(location, order, "B", 3);
        reserveIn(location, order, "A", 12);
        reserveIn(location, order, "A", 4);

        PickList list = pickLists.createForOrder(order);

        assertThat(list.getLines()).extracting(line -> line.getBin().getCode())
                .containsExactly("A-12", "A-4", "B-3");
    }

    @Test
    void completingAPickListShipsTheReservedStock() {
        Location location = fixtures.location();
        String order = Fixtures.unique("ORD");
        Sku sku = reserveIn(location, order, "C", 1);

        PickList list = pickLists.createForOrder(order);
        PickList completed = pickLists.complete(list.getPublicId());

        assertThat(completed.getStatus()).isEqualTo(PickListStatus.COMPLETED);
        assertThat(stock.onHand(sku.getId())).isEqualTo(4);
        assertThat(reservations.activeForOrder(order)).isEmpty();
    }

    @Test
    void oneOpenPickListPerOrder() {
        Location location = fixtures.location();
        String order = Fixtures.unique("ORD");
        reserveIn(location, order, "A", 1);
        pickLists.createForOrder(order);

        assertThatThrownBy(() -> pickLists.createForOrder(order)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void orderWithoutReservationsCannotBePicked() {
        assertThatThrownBy(() -> pickLists.createForOrder(Fixtures.unique("ORD")))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void cancelledListsCannotBeCompleted() {
        Location location = fixtures.location();
        String order = Fixtures.unique("ORD");
        reserveIn(location, order, "A", 2);
        PickList list = pickLists.createForOrder(order);

        pickLists.cancel(list.getPublicId());

        assertThatThrownBy(() -> pickLists.complete(list.getPublicId())).isInstanceOf(InvalidStateException.class);
    }

    private Sku reserveIn(Location location, String order, String aisle, int bay) {
        Sku sku = fixtures.sku(0);
        Long bin = fixtures.bin(location, aisle, bay).getId();
        stock.receive(sku.getId(), bin, 5, "OPENING");
        reservations.reserve(order, sku.getId(), 1);
        return sku;
    }

    @Test
    void reservationsOnAPickListStayActiveUntilPicked() {
        Location location = fixtures.location();
        String order = Fixtures.unique("ORD");
        reserveIn(location, order, "E", 5);

        pickLists.createForOrder(order);

        assertThat(reservations.activeForOrder(order)).extracting(Reservation::getStatus)
                .containsOnly(ReservationStatus.ACTIVE);
    }
}
