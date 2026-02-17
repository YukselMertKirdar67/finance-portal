package com.financeportal.backend.Instrument.DTO.External;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcmbCurrency {

    private String code;              // USD, EUR, GBP
    private String name;              // ABD DOLARI
    private String unit;              // 1
    private BigDecimal forexBuying;   // Döviz alış
    private BigDecimal forexSelling;  // Döviz satış
    private BigDecimal banknoteBuying;  // Efektif alış
    private BigDecimal banknoteSelling; // Efektif satış
}
