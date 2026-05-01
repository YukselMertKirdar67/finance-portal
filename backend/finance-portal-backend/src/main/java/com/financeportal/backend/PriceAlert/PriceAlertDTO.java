package com.financeportal.backend.PriceAlert;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlertDTO {
    private Long id;
    private String instrumentSymbol;
    private String instrumentName;
    private BigDecimal targetPrice;
    private AlertCondition condition;
    private boolean active;
    private boolean triggered;
    private LocalDateTime triggeredAt;
    private LocalDateTime createdAt;
}
