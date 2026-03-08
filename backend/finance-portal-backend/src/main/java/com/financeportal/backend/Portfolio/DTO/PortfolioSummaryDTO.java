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
public class PortfolioSummaryDTO {

    private Integer totalPortfolios;
    private BigDecimal totalValue;              // Tüm portföylerin toplam değeri
    private BigDecimal totalInvested;           // Toplam yatırım
    private BigDecimal totalUnrealizedPnL;      // Toplam kar/zarar
    private BigDecimal totalPnLPercent;         // Genel kar/zarar %

    // En iyi/kötü performans gösteren portföyler
    private List<PortfolioDTO> topPerformers;   // En çok kazandıran portföyler (top 3)
    private List<PortfolioDTO> worstPerformers; // En çok kaybettiren portföyler (top 3)

    // Asset allocation (Varlık dağılımı)
    private List<AssetAllocationDTO> assetAllocation;
}
