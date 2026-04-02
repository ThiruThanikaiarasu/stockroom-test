package com.stockroom.api.picking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.stockroom.core.picking.PickList;
import com.stockroom.core.picking.PickListLine;
import com.stockroom.core.picking.PickListService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pick-lists")
public class PickListController {

    private final PickListService pickLists;

    public PickListController(PickListService pickLists) {
        this.pickLists = pickLists;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PickListResponse create(@Valid @RequestBody CreatePickListRequest request) {
        return PickListResponse.from(pickLists.createForOrder(request.orderRef()));
    }

    @GetMapping("/{publicId}")
    public PickListResponse get(@PathVariable UUID publicId) {
        return PickListResponse.from(pickLists.get(publicId));
    }

    @GetMapping
    public List<PickListResponse> open() {
        return pickLists.open().stream().map(PickListResponse::from).toList();
    }

    @GetMapping("/summary")
    public Map<UUID, Integer> summary() {
        return pickLists.lineCounts(pickLists.open());
    }

    @PostMapping("/{publicId}/complete")
    public PickListResponse complete(@PathVariable UUID publicId) {
        return PickListResponse.from(pickLists.complete(publicId));
    }

    @PostMapping("/{publicId}/cancel")
    public PickListResponse cancel(@PathVariable UUID publicId) {
        return PickListResponse.from(pickLists.cancel(publicId));
    }

    public record CreatePickListRequest(@NotBlank String orderRef) {
    }

    public record PickListResponse(UUID id, String orderRef, String status, LocalDateTime createdAt,
                                   List<LineResponse> lines) {
        static PickListResponse from(PickList list) {
            return new PickListResponse(list.getPublicId(), list.getOrderRef(), list.getStatus().name(),
                    list.getCreatedAt(), list.getLines().stream().map(LineResponse::from).toList());
        }
    }

    public record LineResponse(String binCode, String skuCode, int quantity, int picked, String reservation) {
        static LineResponse from(PickListLine line) {
            return new LineResponse(line.getBin().getCode(), line.getSku().getCode(), line.getQuantity(),
                    line.getPicked(), line.getReservation().getReference());
        }
    }
}
