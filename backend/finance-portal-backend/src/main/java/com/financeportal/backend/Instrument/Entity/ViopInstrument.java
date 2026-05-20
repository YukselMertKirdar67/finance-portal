package com.financeportal.backend.Instrument.Entity;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "viop_instrument")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ViopInstrument extends BaseInstrument {

    @Column(length = 50)
    private String underlyingAsset; // Dayanak varlık (BIST30, USD/TRY vb.)

    @Column(length = 10)
    private String contractType; // FUTURES veya OPTION

    @Column
    private java.time.LocalDate expiryDate; // Vade tarihi

    @Column(precision = 18, scale = 2)
    private java.math.BigDecimal initialMargin; // Başlangıç teminatı

    @Override
    public InstrumentType getInstrumentType() {
        return InstrumentType.VIOP;
    }
}
