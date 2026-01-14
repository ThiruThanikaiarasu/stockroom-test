package com.stockroom.testsupport;

import java.util.concurrent.atomic.AtomicInteger;

import com.stockroom.core.location.Bin;
import com.stockroom.core.location.Location;
import com.stockroom.core.location.LocationService;
import com.stockroom.core.sku.NewSku;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.sku.SkuService;
import com.stockroom.core.stock.StockService;
import org.springframework.stereotype.Component;

/**
 * Creates reference data with codes that are unique per call, so tests sharing one application
 * context never collide.
 */
@Component
public class Fixtures {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final SkuService skus;
    private final LocationService locations;
    private final StockService stock;

    public Fixtures(SkuService skus, LocationService locations, StockService stock) {
        this.skus = skus;
        this.locations = locations;
        this.stock = stock;
    }

    public static String unique(String prefix) {
        return prefix + "-" + SEQUENCE.incrementAndGet();
    }

    public Sku sku(int reorderPoint) {
        return skus.create(new NewSku(unique("SKU"), "Test item", "Fixture SKU", "EA", reorderPoint));
    }

    public Location location() {
        return locations.createLocation(unique("WH"), "Test warehouse");
    }

    public Bin bin(Location location, String aisle, int bay) {
        return locations.addBin(location.getId(), aisle, bay);
    }

    public Bin bin() {
        return bin(location(), "A", 1);
    }

    public Bin stocked(Sku sku, int quantity) {
        Bin bin = bin();
        stock.receive(sku.getId(), bin.getId(), quantity, "OPENING");
        return bin;
    }
}
