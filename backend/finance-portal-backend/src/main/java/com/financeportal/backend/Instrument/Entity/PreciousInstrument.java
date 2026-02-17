package com.financeportal.backend.Instrument.Entity;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "precious_instrument")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PreciousInstrument extends BaseInstrument {

    @Column(length = 20)
    private String metalType; // GOLD, SILVER, PLATINUM, PALLADIUM

    @Column(length = 10)
    private String unit; // oz (ons), kg, gram

    @Override
    public InstrumentType getInstrumentType() {
        return InstrumentType.PRECIOUS;
    }
}
