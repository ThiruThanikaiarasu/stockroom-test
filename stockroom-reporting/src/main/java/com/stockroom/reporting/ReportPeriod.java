package com.stockroom.reporting;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A half-open reporting window [from, to) in UTC. */
public record ReportPeriod(LocalDateTime from, LocalDateTime to) {

    public ReportPeriod {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    /** Whole days, inclusive of both dates. */
    public static ReportPeriod ofDays(LocalDate firstDay, LocalDate lastDay) {
        return new ReportPeriod(firstDay.atStartOfDay(), lastDay.plusDays(1).atStartOfDay());
    }
}
