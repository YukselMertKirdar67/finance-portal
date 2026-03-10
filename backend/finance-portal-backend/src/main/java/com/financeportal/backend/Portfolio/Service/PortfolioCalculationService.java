package com.financeportal.backend.Portfolio.Service;

import java.math.BigDecimal;

public interface PortfolioCalculationService {

    /**
     * Calculate new average buy price when adding to a position
     * Formula: ((existing_qty × existing_avg_price) + (new_qty × new_price)) / (existing_qty + new_qty)
     *
     * @param existingQuantity Current quantity
     * @param existingAvgPrice Current average price
     * @param newQuantity New quantity being added
     * @param newPrice New purchase price
     * @return New average buy price
     */
    BigDecimal calculateNewAverageBuyPrice(
            BigDecimal existingQuantity,
            BigDecimal existingAvgPrice,
            BigDecimal newQuantity,
            BigDecimal newPrice
    );

    /**
     * Calculate unrealized P&L
     * Formula: (current_price - avg_buy_price) × quantity
     *
     * @param quantity Holding quantity
     * @param avgBuyPrice Average buy price
     * @param currentPrice Current market price
     * @return Unrealized P&L
     */
    BigDecimal calculateUnrealizedPnL(
            BigDecimal quantity,
            BigDecimal avgBuyPrice,
            BigDecimal currentPrice
    );

    /**
     * Calculate P&L percentage
     * Formula: ((current_price - avg_buy_price) / avg_buy_price) × 100
     *
     * @param avgBuyPrice Average buy price
     * @param currentPrice Current market price
     * @return P&L percentage
     */
    BigDecimal calculatePnLPercent(
            BigDecimal avgBuyPrice,
            BigDecimal currentPrice
    );

    /**
     * Calculate total investment
     * Formula: quantity × avg_buy_price
     *
     * @param quantity Quantity
     * @param avgBuyPrice Average buy price
     * @return Total investment
     */
    BigDecimal calculateTotalInvestment(
            BigDecimal quantity,
            BigDecimal avgBuyPrice
    );

    /**
     * Calculate current value
     * Formula: quantity × current_price
     *
     * @param quantity Quantity
     * @param currentPrice Current price
     * @return Current value
     */
    BigDecimal calculateCurrentValue(
            BigDecimal quantity,
            BigDecimal currentPrice
    );

    /**
     * Calculate net transaction amount (including commission and tax)
     * Formula: total_amount + commission + tax
     *
     * @param totalAmount Base transaction amount
     * @param commission Commission amount
     * @param tax Tax amount
     * @return Net amount
     */
    BigDecimal calculateNetAmount(
            BigDecimal totalAmount,
            BigDecimal commission,
            BigDecimal tax
    );

    /**
     * Validate sell transaction
     * Checks if user has sufficient quantity to sell
     *
     * @param availableQuantity Available quantity in holding
     * @param sellQuantity Quantity to sell
     * @return true if valid, false otherwise
     */
    boolean validateSellQuantity(
            BigDecimal availableQuantity,
            BigDecimal sellQuantity
    );

    /**
     * Calculate portfolio return percentage
     * Formula: ((current_value - initial_balance) / initial_balance) × 100
     *
     * @param initialBalance Initial portfolio balance
     * @param currentValue Current portfolio value
     * @return Return percentage
     */
    BigDecimal calculatePortfolioReturn(
            BigDecimal initialBalance,
            BigDecimal currentValue
    );
}
