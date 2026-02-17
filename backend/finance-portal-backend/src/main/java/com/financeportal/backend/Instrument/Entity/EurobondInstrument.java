package com.financeportal.backend.Instrument.Entity;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "eurobond_instrument")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EurobondInstrument extends BaseInstrument {

    @Column
    private LocalDate maturityDate;

    @Column(precision = 5, scale = 2)
    private BigDecimal couponRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal faceValue;

    @Column(length = 10)
    private String issueCurrency; // USD, EUR

    @Override
    public InstrumentType getInstrumentType() {
        return InstrumentType.EUROBOND;
    }
}

