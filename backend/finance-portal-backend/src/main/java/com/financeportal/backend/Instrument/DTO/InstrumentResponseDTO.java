package com.financeportal.backend.Instrument.DTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class InstrumentResponseDTO implements Serializable {
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

    // ====== FUND (Yatırım Fonları) Alanları ======
    private String fundCode;  // AAK, AHL, vs
    private String fundType;  // Hisse Senedi Fonu, Tahvil Fonu, vs
    private String umbrella;
    private BigDecimal totalValue;
    private Integer investorCount;


    // ====== Fiyat Bilgileri ======
    private PriceDataDTO currentPrice;
}
