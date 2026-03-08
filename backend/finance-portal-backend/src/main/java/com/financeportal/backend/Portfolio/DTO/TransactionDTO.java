package com.financeportal.backend.Portfolio.DTO;

import com.financeportal.backend.Portfolio.Enum.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {

    private Long id;

    // Portfolio info
    private Long portfolioId;
    private String portfolioName;

    // Instrument info
    private Long instrumentId;
    private String instrumentSymbol;
    private String instrumentName;
    private String instrumentType;

    // Transaction details
    private TransactionType transactionType;  // BUY, SELL
    private BigDecimal quantity;
    private BigDecimal price;                 // Birim fiyat
    private BigDecimal totalAmount;           // Toplam tutar
    private BigDecimal commission;
    private BigDecimal tax;
    private BigDecimal netAmount;             // Net tutar (totalAmount + commission + tax)

    private LocalDateTime transactionDate;
    private String notes;

    private LocalDateTime createdAt;
}