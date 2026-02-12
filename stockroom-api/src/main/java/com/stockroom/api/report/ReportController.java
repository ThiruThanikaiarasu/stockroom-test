package com.stockroom.api.report;

import java.time.LocalDate;

import com.stockroom.reporting.ReportPeriod;
import com.stockroom.reporting.RevaluationReport;
import com.stockroom.reporting.RevaluationReportService;
import com.stockroom.reporting.ThroughputReport;
import com.stockroom.reporting.ThroughputReportService;
import com.stockroom.reporting.ValuationReport;
import com.stockroom.reporting.ValuationReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ThroughputReportService throughput;
    private final ValuationReportService valuation;
    private final RevaluationReportService revaluations;

    public ReportController(ThroughputReportService throughput, ValuationReportService valuation,
                            RevaluationReportService revaluations) {
        this.throughput = throughput;
        this.valuation = valuation;
        this.revaluations = revaluations;
    }

    /** Throughput for whole UTC days, both dates inclusive. */
    @GetMapping("/throughput")
    public ThroughputReport throughput(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return throughput.throughput(ReportPeriod.ofDays(from, to));
    }

    @GetMapping("/valuation")
    public ValuationReport valuation() {
        return valuation.valuation();
    }

    @GetMapping("/revaluations/{skuCode}")
    public RevaluationReport revaluations(@PathVariable String skuCode) {
        return revaluations.forSku(skuCode);
    }
}
