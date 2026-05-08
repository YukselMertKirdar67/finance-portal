package com.financeportal.backend.Instrument.Entity;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "fund_instrument")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FundInstrument extends BaseInstrument {

    @Column(length = 50)
    private String fundCode;  // AAK, AHL, vs

    @Column(length = 100)
    private String fundType;  // Hisse Senedi Fonu, Tahvil Fonu, vs

    @Column(length = 100)
    private String umbrella;  // Şemsiye fon adı

    @Column(precision = 18, scale = 6)
    private BigDecimal totalValue;  // Portföy büyüklüğü

    @Column
    private Integer investorCount;  // Yatırımcı sayısı

    @Override
    public InstrumentType getInstrumentType() {
        return InstrumentType.FUND;
    }
}
