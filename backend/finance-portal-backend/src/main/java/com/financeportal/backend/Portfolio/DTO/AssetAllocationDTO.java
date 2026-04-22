package com.financeportal.backend.Portfolio.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetAllocationDTO {

    private String instrumentType;      // FOREX, STOCK, CRYPTO, etc.
    private BigDecimal totalValue;      // Bu tipteki toplam değer
    private BigDecimal percentage;      // Yüzde (%)
    private Integer count;              // Bu tipteki varlık sayısı
}
