package com.stockroom.core.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import com.stockroom.core.location.Bin;
import com.stockroom.core.location.Location;
import com.stockroom.core.sku.Sku;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class StockLevelRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    StockLevelRepository levels;

    @Test
    void availabilitySumsAcrossBinsNetOfReservations() {
        Sku sku = em.persist(new Sku("REPO-1", "Repo test", "EA", 0, LocalDateTime.of(2026, 1, 1, 0, 0)));
        Location location = em.persist(new Location("REPO", "Repo DC"));
        StockLevel first = new StockLevel(sku, em.persist(new Bin(location, "A", 1)));
        first.add(10);
        first.reserve(4);
        StockLevel second = new StockLevel(sku, em.persist(new Bin(location, "A", 2)));
        second.add(3);
        em.persist(first);
        em.persist(second);
        em.flush();

        assertThat(levels.onHandForSku(sku.getId())).isEqualTo(13);
        assertThat(levels.availableForSku(sku.getId())).isEqualTo(9);
    }

    @Test
    void unknownSkuHasNothingAvailable() {
        assertThat(levels.availableForSku(-5L)).isZero();
        assertThat(levels.onHandForSku(-5L)).isZero();
    }

    @Test
    void levelsAreReturnedWithTheirBins() {
        Sku sku = em.persist(new Sku("REPO-2", "Repo test", "EA", 0, LocalDateTime.of(2026, 1, 1, 0, 0)));
        Location location = em.persist(new Location("REPO2", "Repo DC"));
        em.persist(new StockLevel(sku, em.persist(new Bin(location, "C", 1))));
        em.persist(new StockLevel(sku, em.persist(new Bin(location, "B", 1))));
        em.flush();
        em.clear();

        assertThat(levels.findBySkuId(sku.getId())).extracting(level -> level.getBin().getCode())
                .containsExactly("B-1", "C-1");
    }
}
