package com.financeportal.backend.Portfolio.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceDataPointDTO {

    private LocalDate date;
    private BigDecimal value;           // Portföy değeri
    private BigDecimal returnPercent;   // O güne kadar getiri %
}
