package com.financeportal.backend.Portfolio.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioPerformanceDTO {

    private Long portfolioId;
    private String portfolioName;

    // Performance metrics
    private BigDecimal dailyReturn;         // Günlük getiri %
    private BigDecimal weeklyReturn;        // Haftalık getiri %
    private BigDecimal monthlyReturn;       // Aylık getiri %
    private BigDecimal yearlyReturn;        // Yıllık getiri %
    private BigDecimal totalReturn;         // Toplam getiri %

    // Historical data (grafik için)
    private List<PerformanceDataPointDTO> historicalData;
}
