package com.financeportal.backend.Instrument.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "instrument_prices", indexes = {
        @Index(name = "idx_instrument_timestamp", columnList = "instrument_id,timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private BaseInstrument instrument;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal openPrice;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal highPrice;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal lowPrice;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal previousClose;

    @Column(precision = 18, scale = 6)
    private BigDecimal changeAmount;

    @Column(precision = 10, scale = 4)
    private BigDecimal changePercent;

    @Column
    private Long volume;

    // ✅ Tahvil için ek alan
    @Column(precision = 10, scale = 4)
    private BigDecimal yieldRate;  // Getiri oranı

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        calculateChanges();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateChanges();
    }

    private void calculateChanges() {
        if (previousClose != null && currentPrice != null &&
                previousClose.compareTo(BigDecimal.ZERO) > 0) {

            changeAmount = currentPrice.subtract(previousClose);
            changePercent = changeAmount
                    .divide(previousClose, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }
}