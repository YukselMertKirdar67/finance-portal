package com.financeportal.backend.PriceAlert;

import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "price_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private BaseInstrument instrument;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertCondition condition; // ABOVE, BELOW

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean triggered = false;

    @Column
    private LocalDateTime triggeredAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
