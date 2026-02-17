package com.financeportal.backend.Instrument.Entity;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_instrument")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StockInstrument extends BaseInstrument {

    @Column(length = 50)
    private String sector; // Havacılık, Finans, Teknoloji

    @Column(precision = 18, scale = 2)
    private BigDecimal marketCap; // Piyasa değeri (opsiyonel)

    @Override
    public InstrumentType getInstrumentType() {
        return InstrumentType.STOCK;
    }
}
