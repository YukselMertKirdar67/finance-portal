package com.financeportal.backend.Instrument.Entity;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bond_instrument")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BondInstrument extends BaseInstrument {

    @Column
    private LocalDate maturityDate; // Vade tarihi

    @Column(precision = 5, scale = 2)
    private BigDecimal couponRate; // Kupon oranı (%)

    @Column(precision = 10, scale = 2)
    private BigDecimal faceValue; // Nominal değer

    @Column(length = 50)
    private String issuer; // İhraççı: Hazine, şirket

    @Override
    public InstrumentType getInstrumentType() {
        return InstrumentType.BOND;
    }
}
