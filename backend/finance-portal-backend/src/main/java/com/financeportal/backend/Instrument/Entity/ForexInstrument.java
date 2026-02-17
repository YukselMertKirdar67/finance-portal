package com.financeportal.backend.Instrument.Entity;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "forex_instrument")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ForexInstrument extends BaseInstrument {

    @Column(length = 10)
    private String baseCurrency; // USD

    @Column(length = 10)
    private String quoteCurrency; // TRY

    @Override
    public InstrumentType getInstrumentType() {
        return InstrumentType.FOREX;
    }
}
