package com.stockroom.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ReportPeriodTest {

    @Test
    void wholeDaysIncludeTheLastDay() {
        ReportPeriod period = ReportPeriod.ofDays(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(period.from()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
        assertThat(period.to()).isEqualTo(LocalDateTime.of(2026, 4, 1, 0, 0));
    }

    @Test
    void singleDayPeriod() {
        LocalDate day = LocalDate.of(2026, 2, 28);

        ReportPeriod period = ReportPeriod.ofDays(day, day);

        assertThat(period.to()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
    }

    @Test
    void rejectsReversedPeriods() {
        assertThatThrownBy(() -> ReportPeriod.ofDays(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingBounds() {
        assertThatThrownBy(() -> new ReportPeriod(null, LocalDateTime.of(2026, 1, 1, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
