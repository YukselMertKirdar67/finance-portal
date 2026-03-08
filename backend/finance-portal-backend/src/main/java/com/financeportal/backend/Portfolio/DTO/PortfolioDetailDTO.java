package com.financeportal.backend.Portfolio.DTO;

import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioDetailDTO {

    private Long id;
    private String name;
    private String description;
    private PortfolioType portfolioType;
    private BigDecimal initialBalance;
    private String currency;
    private Boolean active;

    // Holdings (Portföydeki varlıklar)
    private List<HoldingDTO> holdings;

    // Summary (Özet bilgiler - calculated)
    private BigDecimal totalInvested;       // Toplam yatırım
    private BigDecimal currentValue;        // Güncel toplam değer
    private BigDecimal unrealizedPnL;       // Gerçekleşmemiş kar/zarar
    private BigDecimal pnlPercent;          // Kar/zarar yüzdesi
    private BigDecimal cashBalance;         // Nakit bakiye (initialBalance - totalInvested)

    // Statistics
    private Integer totalHoldings;          // Toplam varlık sayısı
    private Integer totalTransactions;      // Toplam işlem sayısı

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
