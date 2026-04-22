package com.financeportal.backend.Portfolio.DTO;

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
public class HoldingDTO {

    private Long holdingId;

    // Instrument bilgileri
    private Long instrumentId;
    private String instrumentSymbol;
    private String instrumentName;
    private String instrumentType;

    // Holding bilgileri
    private BigDecimal quantity;            // Miktar
    private BigDecimal averageBuyPrice;     // Ortalama alış fiyatı

    // Current price (runtime'da instrument_prices'dan çekilir)
    private BigDecimal currentPrice;        // Güncel fiyat

    private String currency;        // Enstrümanın para birimi
    private BigDecimal exchangeRate; // Alım anındaki kur

    // Calculated fields
    private BigDecimal totalInvestment;     // Toplam yatırım (quantity × avgBuyPrice)
    private BigDecimal currentValue;        // Güncel değer (quantity × currentPrice)
    private BigDecimal unrealizedPnL;       // Gerçekleşmemiş kar/zarar
    private BigDecimal pnlPercent;          // Kar/zarar yüzdesi

    // Dates
    private LocalDateTime firstPurchaseDate;
    private LocalDateTime lastPurchaseDate;
}
