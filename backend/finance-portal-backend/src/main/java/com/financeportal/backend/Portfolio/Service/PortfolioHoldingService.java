package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Portfolio.DTO.AssetAllocationDTO;
import com.financeportal.backend.Portfolio.DTO.HoldingDTO;

import java.math.BigDecimal;
import java.util.List;

public interface PortfolioHoldingService {

    /**
     * Get all holdings for a specific portfolio
     *
     * @param portfolioId Portfolio ID
     * @return List of holding DTOs with current prices and calculated metrics
     */
    List<HoldingDTO> getHoldingsByPortfolioId(Long portfolioId);

    /**
     * Get a specific holding
     *
     * @param holdingId Holding ID
     * @return Holding DTO
     */
    HoldingDTO getHoldingById(Long holdingId);

    /**
     * Get holding by portfolio and instrument
     *
     * @param portfolioId Portfolio ID
     * @param instrumentId Instrument ID
     * @return Holding DTO if exists, null otherwise
     */
    HoldingDTO getHoldingByPortfolioAndInstrument(Long portfolioId, Long instrumentId);

    /**
     * Get active holdings only (quantity > 0)
     *
     * @param portfolioId Portfolio ID
     * @return List of active holdings
     */
    List<HoldingDTO> getActiveHoldings(Long portfolioId);

    /**
     * Get top holdings by value
     *
     * @param portfolioId Portfolio ID
     * @param limit Number of top holdings to return
     * @return List of top holdings
     */
    List<HoldingDTO> getTopHoldingsByValue(Long portfolioId, int limit);

    /**
     * Get asset allocation for a portfolio
     * Groups holdings by instrument type
     *
     * @param portfolioId Portfolio ID
     * @return List of asset allocation DTOs
     */
    List<AssetAllocationDTO> getAssetAllocation(Long portfolioId);

    /**
     * Calculate total investment for a portfolio
     * Sum of (quantity × average buy price) for all holdings
     *
     * @param portfolioId Portfolio ID
     * @return Total invested amount
     */
    BigDecimal calculateTotalInvestment(Long portfolioId);

    /**
     * Calculate current value for a portfolio
     * Sum of (quantity × current price) for all holdings
     *
     * @param portfolioId Portfolio ID
     * @return Current total value
     */
    BigDecimal calculateCurrentValue(Long portfolioId);

    /**
     * Calculate unrealized P&L for a portfolio
     * Current Value - Total Investment
     *
     * @param portfolioId Portfolio ID
     * @return Unrealized P&L
     */
    BigDecimal calculateUnrealizedPnL(Long portfolioId);

    /**
     * Calculate unrealized P&L for a specific holding
     *
     * @param holdingId Holding ID
     * @return Unrealized P&L
     */
    BigDecimal calculateHoldingUnrealizedPnL(Long holdingId);

    /**
     * Check if a holding exists for portfolio and instrument
     *
     * @param portfolioId Portfolio ID
     * @param instrumentId Instrument ID
     * @return true if holding exists, false otherwise
     */
    boolean holdingExists(Long portfolioId, Long instrumentId);

    /**
     * Delete a holding
     *
     * @param holdingId Holding ID
     */
    void deleteHolding(Long holdingId);

    /**
     * Delete holdings with zero quantity
     * Cleanup operation
     *
     * @param portfolioId Portfolio ID
     * @return Number of holdings deleted
     */
    int deleteZeroQuantityHoldings(Long portfolioId);
}
