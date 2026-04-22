package com.financeportal.backend.Instrument.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentUpdateStatusDTO {

    private boolean updating;
    private LocalDateTime lastUpdateTime;
    private Integer totalUpdated;
    private Integer tcmbUpdated;
    private Integer yahooUpdated;
    private Integer bistUpdated;
    private Integer preciousUpdated;
    private Integer bondsUpdated;
    private String message;
}
