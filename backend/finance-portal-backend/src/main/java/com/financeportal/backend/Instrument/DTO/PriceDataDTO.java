package com.financeportal.backend.Instrument.DTO;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceDataDTO implements Serializable {
    private BigDecimal current;
    private String currency; // Orijinal para birimi
    private BigDecimal exchangeRate; // Kullanılan kur
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal previousClose;
    private BigDecimal changeAmount;
    private BigDecimal changePercent;
    private Long volume;
    private BigDecimal yieldRate;
    private LocalDateTime timestamp;
}
