package com.financeportal.backend.PriceAlert;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePriceAlertRequestDTO {

    @NotNull
    private Long instrumentId;

    @NotNull
    @Positive
    private BigDecimal targetPrice;

    @NotNull
    private AlertCondition condition;
}
