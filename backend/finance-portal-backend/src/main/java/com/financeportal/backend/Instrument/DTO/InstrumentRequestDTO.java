package com.financeportal.backend.Instrument.DTO;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentRequestDTO {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Type is required")
    private InstrumentType type;

    private String exchange;
    private String description;
    private String currency;

    // ====== STOCK (Hisse Senedi) Alanları ======
    private String sector;
    private BigDecimal marketCap;

    // ====== BOND (Tahvil) Alanları ======
    private LocalDate maturityDate;
    private BigDecimal couponRate;
    private BigDecimal faceValue;
    private String issuer;

    // ====== FOREX (Döviz) Alanları ======
    private String baseCurrency;
    private String quoteCurrency;

    // ====== CRYPTO (Kripto) Alanları ======
    private String blockchain;
    private BigDecimal totalSupply;
    private BigDecimal circulatingSupply;

    // ====== PRECIOUS (Değerli Maden) Alanları ======
    private String metalType;
    private String unit;

    // ====== FUND (Yatırım Fonları) Alanları ======
    private String fundCode;  // AAK, AHL, vs
    private String fundType;  // Hisse Senedi Fonu, Tahvil Fonu, vs
    private String umbrella;
    private BigDecimal totalValue;
    private Integer investorCount;
}
