package com.financeportal.backend.Instrument.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceDataDTO {
    private BigDecimal current;
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
