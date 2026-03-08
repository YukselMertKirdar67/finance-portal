package com.financeportal.backend.Portfolio.DTO;

import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioDTO {

    private Long id;
    private String name;
    private String description;
    private PortfolioType portfolioType;
    private BigDecimal initialBalance;
    private String currency;
    private Boolean active;

    // Calculated fields (runtime'da hesaplanır)
    private BigDecimal totalValue;          // Portföyün güncel toplam değeri
    private BigDecimal totalInvested;       // Toplam yatırılan miktar
    private BigDecimal unrealizedPnL;       // Gerçekleşmemiş kar/zarar
    private BigDecimal pnlPercent;          // Kar/zarar yüzdesi
    private Integer holdingCount;           // Portföydeki varlık sayısı

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
