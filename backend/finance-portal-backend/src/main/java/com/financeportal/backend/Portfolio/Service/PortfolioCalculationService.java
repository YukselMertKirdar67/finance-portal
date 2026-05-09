package com.financeportal.backend.Portfolio.Service;

import java.math.BigDecimal;

public interface PortfolioCalculationService {

    BigDecimal calculateNewAverageBuyPrice(
            BigDecimal existingQuantity,
            BigDecimal existingAvgPrice,
            BigDecimal newQuantity,
            BigDecimal newPrice
    );

    BigDecimal calculateUnrealizedPnL(
            BigDecimal quantity,
            BigDecimal avgBuyPrice,
            BigDecimal currentPrice
    );

    BigDecimal calculatePnLPercent(
            BigDecimal avgBuyPrice,
            BigDecimal currentPrice
    );

    BigDecimal calculateTotalInvestment(
            BigDecimal quantity,
            BigDecimal avgBuyPrice
    );

    BigDecimal calculateCurrentValue(
            BigDecimal quantity,
            BigDecimal currentPrice
    );

    BigDecimal calculateNetAmount(
            BigDecimal totalAmount,
            BigDecimal commission,
            BigDecimal tax
    );

    boolean validateSellQuantity(
            BigDecimal availableQuantity,
            BigDecimal sellQuantity
    );

    BigDecimal calculatePortfolioReturn(
            BigDecimal initialBalance,
            BigDecimal currentValue
    );
}
