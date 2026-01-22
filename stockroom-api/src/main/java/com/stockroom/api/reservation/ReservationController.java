package com.stockroom.api.reservation;

import java.time.LocalDateTime;
import java.util.List;

import com.stockroom.core.reservation.Reservation;
import com.stockroom.core.reservation.ReservationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Valid @RequestBody ReserveRequest request) {
        return ReservationResponse.from(reservations.reserve(request.orderRef(), request.skuId(), request.quantity()));
    }

    @GetMapping("/{reference}")
    public ReservationResponse get(@PathVariable String reference) {
        return ReservationResponse.from(reservations.get(reference));
    }

    @GetMapping
    public List<ReservationResponse> forOrder(@RequestParam String orderRef) {
        return reservations.activeForOrder(orderRef).stream().map(ReservationResponse::from).toList();
    }

    @PostMapping("/{reference}/release")
    public ReservationResponse release(@PathVariable String reference) {
        return ReservationResponse.from(reservations.release(reference));
    }

    public record ReserveRequest(@NotBlank String orderRef, @NotNull Long skuId, @Positive int quantity) {
    }

    public record ReservationResponse(String reference, String orderRef, String skuCode, String binCode,
                                      int quantity, String status, LocalDateTime createdAt,
                                      LocalDateTime expiresAt) {
        static ReservationResponse from(Reservation r) {
            return new ReservationResponse(r.getReference(), r.getOrderRef(), r.getSku().getCode(),
                    r.getBin().getCode(), r.getQuantity(), r.getStatus().name(), r.getCreatedAt(), r.getExpiresAt());
        }
    }
}
