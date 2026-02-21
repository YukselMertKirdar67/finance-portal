package com.financeportal.backend.Comparison;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonDTO {

    private InstrumentInfo instrument1;
    private InstrumentInfo instrument2;
    private List<ComparisonDataPoint> historicalData;
    private PerformanceMetrics metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstrumentInfo {
        private Long id;
        private String symbol;
        private String name;
        private String type;
        private String currency;
        private BigDecimal currentPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonDataPoint {
        private LocalDate date;
        private BigDecimal price1;
        private BigDecimal price2;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private MetricData instrument1Metrics;
        private MetricData instrument2Metrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricData {
        private BigDecimal dailyChange;        // %
        private BigDecimal weeklyChange;       // %
        private BigDecimal monthlyChange;      // %
        private BigDecimal volatility;         // %
        private BigDecimal highestPrice;
        private BigDecimal lowestPrice;
        private BigDecimal priceRange;         // high - low
    }
}

