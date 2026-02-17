package com.financeportal.backend.Instrument.DTO.External;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwelveDataPriceDTO implements Serializable {
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private LocalDateTime timestamp;
}
