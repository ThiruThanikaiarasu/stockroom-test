package com.stockroom.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import com.stockroom.StockroomTestApplication;
import com.stockroom.core.location.Bin;
import com.stockroom.core.reservation.ReservationService;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.stock.StockService;
import com.stockroom.testsupport.Fixtures;
import com.stockroom.testsupport.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ThroughputReportServiceTest {

    @Autowired
    ThroughputReportService reports;

    @Autowired
    StockService stock;

    @Autowired
    ReservationService reservations;

    @Autowired
    Fixtures fixtures;

    @Autowired
    MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.set(StockroomTestApplication.START);
    }

    @Test
    void countsReceiptsAndPicks() {
        LocalDate day = at("2026-05-04T08:00:00Z");
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.stocked(sku, 40);
        reservations.reserve("ORD-1", sku.getId(), 15);
        stock.pickReserved(sku.getId(), bin.getId(), 15, "ORD-1");

        ThroughputReport report = reports.throughput(ReportPeriod.ofDays(day, day));

        assertThat(report.receivedUnits()).isEqualTo(40);
        assertThat(report.pickedUnits()).isEqualTo(15);
        assertThat(report.transferredUnits()).isZero();
        assertThat(report.bySku()).singleElement().satisfies(row -> {
            assertThat(row.unitsHandled()).isEqualTo(55);
            assertThat(row.movements()).isEqualTo(2);
        });
    }

    @Test
    void adjustmentsAreNetted() {
        LocalDate day = at("2026-05-05T08:00:00Z");
        Sku sku = fixtures.sku(0);
        Bin bin = fixtures.stocked(sku, 10);
        stock.adjust(sku.getId(), bin.getId(), -4, "damaged");
        stock.adjust(sku.getId(), bin.getId(), 1, "found");

        ThroughputReport report = reports.throughput(ReportPeriod.ofDays(day, day));

        assertThat(report.adjustedUnits()).isEqualTo(-3);
    }

    @Test
    void skusAreRankedByUnitsHandled() {
        LocalDate day = at("2026-05-06T08:00:00Z");
        Sku slow = fixtures.sku(0);
        Sku fast = fixtures.sku(0);
        fixtures.stocked(slow, 3);
        fixtures.stocked(fast, 300);

        ThroughputReport report = reports.throughput(ReportPeriod.ofDays(day, day));

        assertThat(report.bySku()).extracting(ThroughputReport.SkuThroughput::skuCode)
                .containsExactly(fast.getCode(), slow.getCode());
    }

    @Test
    void movementsOutsideThePeriodAreIgnored() {
        at("2026-05-07T23:59:00Z");
        Sku sku = fixtures.sku(0);
        fixtures.stocked(sku, 8);
        LocalDate nextDay = at("2026-05-08T00:01:00Z");

        ThroughputReport report = reports.throughput(ReportPeriod.ofDays(nextDay, nextDay));

        assertThat(report.receivedUnits()).isZero();
        assertThat(report.bySku()).isEmpty();
    }

    private LocalDate at(String instant) {
        Instant now = Instant.parse(instant);
        clock.set(now);
        return LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
    }
}
