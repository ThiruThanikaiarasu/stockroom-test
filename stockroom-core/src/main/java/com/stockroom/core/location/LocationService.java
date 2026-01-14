package com.stockroom.core.location;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.stockroom.core.common.InvalidStateException;
import com.stockroom.core.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LocationService {

    private static final Pattern AISLE = Pattern.compile("[A-Z]{1,4}");

    private final LocationRepository locations;
    private final BinRepository bins;

    public LocationService(LocationRepository locations, BinRepository bins) {
        this.locations = locations;
        this.bins = bins;
    }

    public Location createLocation(String code, String name) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        locations.findByCode(normalized).ifPresent(existing -> {
            throw new InvalidStateException("Location " + normalized + " already exists");
        });
        return locations.save(new Location(normalized, name));
    }

    @Transactional(readOnly = true)
    public Location getLocation(Long id) {
        return locations.findById(id).orElseThrow(() -> new NotFoundException("Location", id));
    }

    @Transactional(readOnly = true)
    public List<Location> listLocations() {
        return locations.findAll();
    }

    public Bin addBin(Long locationId, String aisle, int bay) {
        Location location = getLocation(locationId);
        String normalizedAisle = aisle.trim().toUpperCase(Locale.ROOT);
        if (!AISLE.matcher(normalizedAisle).matches()) {
            throw new IllegalArgumentException("aisle must be 1-4 letters");
        }
        if (bay < 1 || bay > 999) {
            throw new IllegalArgumentException("bay must be between 1 and 999");
        }
        Bin bin = new Bin(location, normalizedAisle, bay);
        bins.findByLocationIdAndCode(locationId, bin.getCode()).ifPresent(existing -> {
            throw new InvalidStateException("Bin " + bin.getCode() + " already exists");
        });
        return bins.save(bin);
    }

    @Transactional(readOnly = true)
    public Bin getBin(Long id) {
        return bins.findById(id).orElseThrow(() -> new NotFoundException("Bin", id));
    }

    @Transactional(readOnly = true)
    public List<Bin> listBins(Long locationId) {
        getLocation(locationId);
        return bins.findByLocationIdOrderByAisleAscBayAsc(locationId);
    }

    public Bin setPickable(Long binId, boolean pickable) {
        Bin bin = getBin(binId);
        bin.setPickable(pickable);
        return bin;
    }
}
