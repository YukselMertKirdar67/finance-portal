package com.financeportal.backend.Portfolio.Entity;


import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_holdings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_portfolio_instrument",
                columnNames = {"portfolio_id", "instrument_id"}
        ))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_holding_portfolio"))
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_holding_instrument"))
    private BaseInstrument instrument;

    @Column(name = "quantity", precision = 18, scale = 8, nullable = false)
    private BigDecimal quantity; // Miktar (örn: 10.5 adet)

    @Column(name = "average_buy_price", precision = 18, scale = 6, nullable = false)
    private BigDecimal averageBuyPrice; // Ortalama alış fiyatı

    @Column(name = "currency", length = 10)
    private String currency; // TRY, USD, EUR, GBP

    @Column(name = "exchange_rate", precision = 18, scale = 6)
    private BigDecimal exchangeRate; // Alım anındaki kur (TRY karşılığı)

    @Column(name = "first_purchase_date", nullable = false)
    private LocalDateTime firstPurchaseDate; // İlk alım tarihi

    @Column(name = "last_purchase_date")
    private LocalDateTime lastPurchaseDate; // Son alım tarihi

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business logic helper methods

    /**
     * Toplam yatırım tutarı (maliyet)
     * Total Investment = Quantity × Average Buy Price
     */
    public BigDecimal getTotalInvestment() {
        return quantity.multiply(averageBuyPrice);
    }

    /**
     * Güncel değer hesaplama (current price parametre olarak alınır)
     * Current Value = Quantity × Current Price
     */
    public BigDecimal getCurrentValue(BigDecimal currentPrice) {
        return quantity.multiply(currentPrice);
    }

    /**
     * Gerçekleşmemiş kar/zarar (unrealized P&L)
     * Unrealized P&L = (Current Price - Average Buy Price) × Quantity
     */
    public BigDecimal getUnrealizedPnL(BigDecimal currentPrice) {
        return currentPrice.subtract(averageBuyPrice).multiply(quantity);
    }

    /**
     * Kar/Zarar yüzdesi
     * P&L % = ((Current Price - Average Buy Price) / Average Buy Price) × 100
     */
    public BigDecimal getPnLPercent(BigDecimal currentPrice) {
        if (averageBuyPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(averageBuyPrice)
                .divide(averageBuyPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}

