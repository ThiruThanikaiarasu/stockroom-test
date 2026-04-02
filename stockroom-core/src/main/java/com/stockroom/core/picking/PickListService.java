package com.stockroom.core.picking;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.stockroom.core.common.InvalidStateException;
import com.stockroom.core.common.NotFoundException;
import com.stockroom.core.reservation.Reservation;
import com.stockroom.core.reservation.ReservationService;
import com.stockroom.core.stock.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PickListService {

    private static final Comparator<PickListLine> PICK_ORDER =
            Comparator.comparing((PickListLine line) -> line.getBin().getCode());

    private final PickListRepository pickLists;
    private final ReservationService reservations;
    private final StockService stock;
    private final Clock clock;

    public PickListService(PickListRepository pickLists, ReservationService reservations, StockService stock,
                           Clock clock) {
        this.pickLists = pickLists;
        this.reservations = reservations;
        this.stock = stock;
        this.clock = clock;
    }

    /** Builds one pick list covering every active reservation of an order. */
    public PickList createForOrder(String orderRef) {
        if (pickLists.existsByOrderRefAndStatus(orderRef, PickListStatus.OPEN)) {
            throw new InvalidStateException("Order " + orderRef + " already has an open pick list");
        }
        List<Reservation> active = reservations.activeForOrder(orderRef);
        if (active.isEmpty()) {
            throw new InvalidStateException("Order " + orderRef + " has no active reservations");
        }
        PickList pickList = new PickList(orderRef, LocalDateTime.now(clock));
        sequence(active.stream().map(PickListLine::new).toList()).forEach(pickList::addLine);
        return pickLists.save(pickList);
    }

    /** Orders lines in the sequence a picker walks them. */
    static List<PickListLine> sequence(Collection<PickListLine> lines) {
        return lines.stream().sorted(PICK_ORDER).toList();
    }

    @Transactional(readOnly = true)
    public PickList get(UUID publicId) {
        return pickLists.findByPublicId(publicId).orElseThrow(() -> new NotFoundException("Pick list", publicId));
    }

    @Transactional(readOnly = true)
    public List<PickList> open() {
        return pickLists.findByStatusOrderByCreatedAtAsc(PickListStatus.OPEN);
    }

    /** Confirms every line as picked: stock leaves the bins and the reservations are fulfilled. */
    public PickList complete(UUID publicId) {
        PickList pickList = get(publicId);
        pickList.requireOpen();
        for (PickListLine line : pickList.getLines()) {
            stock.pickReserved(line.getSku().getId(), line.getBin().getId(), line.getQuantity(),
                    pickList.getOrderRef());
            line.getReservation().fulfil();
            line.markPicked();
        }
        pickList.complete();
        return pickList;
    }

    public PickList cancel(UUID publicId) {
        PickList pickList = get(publicId);
        pickList.cancel();
        return pickList;
    }

    /** Number of lines on each pick list, keyed by public id. */
    public Map<UUID, Integer> lineCounts(Collection<PickList> lists) {
        Map<UUID, Integer> counts = new HashMap<>();
        for (PickList list : lists) {
            counts.put(list.getPublicId(), list.getLines().size());
        }
        return counts;
    }
}
