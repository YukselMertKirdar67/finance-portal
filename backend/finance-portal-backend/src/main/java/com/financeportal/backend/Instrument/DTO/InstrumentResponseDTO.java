package com.financeportal.backend.Instrument.DTO;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentResponseDTO {
    private Long id;
    private String symbol;
    private String name;
    private InstrumentType type;
    private String exchange;
    private String description;
    private String currency;
    private boolean active;

    // ====== STOCK (Hisse Senedi) Alanları ======
    private String sector;
    private BigDecimal marketCap;

    // ====== BOND (Tahvil) Alanları ======
    private LocalDate maturityDate;
    private BigDecimal couponRate;
    private BigDecimal faceValue;
    private String issuer;  // İhraççı

    // ====== EUROBOND Alanları ======
    private String issueCurrency;  // USD, EUR

    // ====== FOREX (Döviz) Alanları ======
    private String baseCurrency;   // USD
    private String quoteCurrency;  // TRY

    // ====== CRYPTO (Kripto) Alanları ======
    private String blockchain;
    private BigDecimal totalSupply;
    private BigDecimal circulatingSupply;

    // ====== PRECIOUS (Değerli Maden) Alanları ======
    private String metalType;  // GOLD, SILVER, PLATINUM
    private String unit;       // oz, kg, gram

    // ====== Fiyat Bilgileri ======
    private PriceDataDTO currentPrice;
}
