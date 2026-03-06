package com.financeportal.backend.Portfolio.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummaryDTO {

    private Integer totalTransactions;
    private Integer buyTransactions;
    private Integer sellTransactions;

    private BigDecimal totalBuyAmount;      // Toplam alım tutarı
    private BigDecimal totalSellAmount;     // Toplam satış tutarı
    private BigDecimal totalCommission;     // Toplam komisyon
    private BigDecimal totalTax;            // Toplam vergi

    private BigDecimal realizedPnL;         // Gerçekleşmiş kar/zarar (sell - buy)
}
