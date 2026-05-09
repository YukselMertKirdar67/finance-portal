package com.financeportal.backend.WebSocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdateMessage {
    private Long instrumentId;
    private String symbol;
    private String name;
    private String type;
    private BigDecimal currentPrice;
    private BigDecimal changeAmount;
    private BigDecimal changePercent;
    private BigDecimal previousClose;
    private LocalDateTime timestamp;
}
