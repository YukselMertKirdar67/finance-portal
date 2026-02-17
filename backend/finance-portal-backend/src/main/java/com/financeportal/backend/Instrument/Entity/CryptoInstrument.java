package com.financeportal.backend.Instrument.Entity;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "crypto_instrument")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CryptoInstrument extends BaseInstrument {

    @Column(length = 50)
    private String blockchain; // Ethereum, Bitcoin, Binance Smart Chain

    @Column(precision = 25, scale = 8)
    private BigDecimal totalSupply; // Toplam arz

    @Column(precision = 25, scale = 8)
    private BigDecimal circulatingSupply; // Dolaşımdaki miktar

    @Override
    public InstrumentType getInstrumentType() {
        return InstrumentType.CRYPTO;
    }
}
