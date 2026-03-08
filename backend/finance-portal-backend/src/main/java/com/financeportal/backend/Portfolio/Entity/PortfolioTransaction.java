package com.financeportal.backend.Portfolio.Entity;

import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_transactions",
        indexes = {
                @Index(name = "idx_transaction_portfolio", columnList = "portfolio_id"),
                @Index(name = "idx_transaction_date", columnList = "transaction_date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_portfolio"))
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_instrument"))
    private BaseInstrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 10)
    private TransactionType transactionType; // BUY, SELL

    @Column(name = "quantity", precision = 18, scale = 8, nullable = false)
    private BigDecimal quantity; // İşlem miktarı

    @Column(name = "price", precision = 18, scale = 6, nullable = false)
    private BigDecimal price; // İşlem fiyatı (birim fiyat)

    @Column(name = "total_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount; // Toplam tutar (quantity × price)

    @Column(name = "commission", precision = 18, scale = 2)
    private BigDecimal commission; // Komisyon

    @Column(name = "tax", precision = 18, scale = 2)
    private BigDecimal tax; // Vergi

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate; // İşlem tarihi

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // İşlem notu (opsiyonel)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        // Total amount otomatik hesaplama
        if (totalAmount == null) {
            totalAmount = quantity.multiply(price);
        }
    }

    // Business logic helper methods

    /**
     * Net tutar (komisyon ve vergi dahil)
     * Net Amount = Total Amount + Commission + Tax
     */
    public BigDecimal getNetAmount() {
        BigDecimal net = totalAmount;
        if (commission != null) {
            net = net.add(commission);
        }
        if (tax != null) {
            net = net.add(tax);
        }
        return net;
    }

    /**
     * İşlem tipine göre net etki (BUY: negatif, SELL: pozitif)
     */
    public BigDecimal getNetImpact() {
        BigDecimal net = getNetAmount();
        return transactionType == TransactionType.BUY ? net.negate() : net;
    }
}