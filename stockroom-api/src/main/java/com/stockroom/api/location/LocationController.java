package com.stockroom.api.location;

import java.util.List;

import com.stockroom.core.location.Bin;
import com.stockroom.core.location.Location;
import com.stockroom.core.location.LocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locations;

    public LocationController(LocationService locations) {
        this.locations = locations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse create(@Valid @RequestBody CreateLocationRequest request) {
        return LocationResponse.from(locations.createLocation(request.code(), request.name()));
    }

    @GetMapping
    public List<LocationResponse> list() {
        return locations.listLocations().stream().map(LocationResponse::from).toList();
    }

    @PostMapping("/{locationId}/bins")
    @ResponseStatus(HttpStatus.CREATED)
    public BinResponse addBin(@PathVariable Long locationId, @Valid @RequestBody CreateBinRequest request) {
        return BinResponse.from(locations.addBin(locationId, request.aisle(), request.bay()));
    }

    @GetMapping("/{locationId}/bins")
    public List<BinResponse> bins(@PathVariable Long locationId) {
        return locations.listBins(locationId).stream().map(BinResponse::from).toList();
    }

    @PutMapping("/bins/{binId}/pickable")
    public BinResponse setPickable(@PathVariable Long binId, @RequestBody PickableRequest request) {
        return BinResponse.from(locations.setPickable(binId, request.pickable()));
    }

    public record CreateLocationRequest(@NotBlank @Size(max = 20) String code, @NotBlank @Size(max = 120) String name) {
    }

    public record CreateBinRequest(@NotBlank @Size(max = 4) String aisle, @Min(1) @Max(999) int bay) {
    }

    public record PickableRequest(boolean pickable) {
    }

    public record LocationResponse(Long id, String code, String name) {
        static LocationResponse from(Location location) {
            return new LocationResponse(location.getId(), location.getCode(), location.getName());
        }
    }

    public record BinResponse(Long id, String code, String aisle, int bay, boolean pickable) {
        static BinResponse from(Bin bin) {
            return new BinResponse(bin.getId(), bin.getCode(), bin.getAisle(), bin.getBay(), bin.isPickable());
        }
    }
}
