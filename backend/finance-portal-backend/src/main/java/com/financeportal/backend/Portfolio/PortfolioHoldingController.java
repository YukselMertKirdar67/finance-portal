package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Portfolio.DTO.AssetAllocationDTO;
import com.financeportal.backend.Portfolio.DTO.HoldingDTO;
import com.financeportal.backend.Portfolio.Service.PortfolioHoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/holdings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolio Holdings", description = "Portfolio holdings management APIs")
public class PortfolioHoldingController {

    private final PortfolioHoldingService holdingService;

    /**
     * Get all holdings for a portfolio
     * GET /api/portfolios/{portfolioId}/holdings
     */
    @GetMapping
    @Operation(summary = "Get portfolio holdings", description = "Get all holdings for a portfolio with current prices")
    public ResponseEntity<List<HoldingDTO>> getHoldings(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId) {
        log.info("API: Fetching holdings for portfolio: {}", portfolioId);

        List<HoldingDTO> holdings = holdingService.getHoldingsByPortfolioId(portfolioId);

        return ResponseEntity.ok(holdings);
    }

    /**
     * Get active holdings only (quantity > 0)
     * GET /api/portfolios/{portfolioId}/holdings/active
     */
    @GetMapping("/active")
    @Operation(summary = "Get active holdings", description = "Get only holdings with quantity > 0")
    public ResponseEntity<List<HoldingDTO>> getActiveHoldings(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId) {
        log.info("API: Fetching active holdings for portfolio: {}", portfolioId);

        List<HoldingDTO> holdings = holdingService.getActiveHoldings(portfolioId);

        return ResponseEntity.ok(holdings);
    }

    /**
     * Get top holdings by value
     * GET /api/portfolios/{portfolioId}/holdings/top
     */
    @GetMapping("/top")
    @Operation(summary = "Get top holdings", description = "Get top N holdings by current value")
    public ResponseEntity<List<HoldingDTO>> getTopHoldings(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Number of top holdings to return")
            @RequestParam(defaultValue = "10") int limit) {
        log.info("API: Fetching top {} holdings for portfolio: {}", limit, portfolioId);

        List<HoldingDTO> holdings = holdingService.getTopHoldingsByValue(portfolioId, limit);

        return ResponseEntity.ok(holdings);
    }

    /**
     * Get holding by ID
     * GET /api/portfolios/{portfolioId}/holdings/{holdingId}
     */
    @GetMapping("/{holdingId}")
    @Operation(summary = "Get holding by ID", description = "Get details of a specific holding")
    public ResponseEntity<HoldingDTO> getHoldingById(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Holding ID")
            @PathVariable Long holdingId) {
        log.info("API: Fetching holding ID: {} for portfolio: {}", holdingId, portfolioId);

        HoldingDTO holding = holdingService.getHoldingById(holdingId);

        return ResponseEntity.ok(holding);
    }

    /**
     * Get asset allocation
     * GET /api/portfolios/{portfolioId}/holdings/asset-allocation
     */
    @GetMapping("/asset-allocation")
    @Operation(summary = "Get asset allocation", description = "Get asset allocation grouped by instrument type")
    public ResponseEntity<List<AssetAllocationDTO>> getAssetAllocation(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId) {
        log.info("API: Fetching asset allocation for portfolio: {}", portfolioId);

        List<AssetAllocationDTO> allocation = holdingService.getAssetAllocation(portfolioId);

        return ResponseEntity.ok(allocation);
    }

    /**
     * Calculate total investment
     * GET /api/portfolios/{portfolioId}/holdings/total-investment
     */
    @GetMapping("/total-investment")
    @Operation(summary = "Calculate total investment", description = "Calculate total invested amount")
    public ResponseEntity<BigDecimal> getTotalInvestment(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId) {
        log.info("API: Calculating total investment for portfolio: {}", portfolioId);

        BigDecimal totalInvestment = holdingService.calculateTotalInvestment(portfolioId);

        return ResponseEntity.ok(totalInvestment);
    }

    /**
     * Calculate current value
     * GET /api/portfolios/{portfolioId}/holdings/current-value
     */
    @GetMapping("/current-value")
    @Operation(summary = "Calculate current value", description = "Calculate total current value")
    public ResponseEntity<BigDecimal> getCurrentValue(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId) {
        log.info("API: Calculating current value for portfolio: {}", portfolioId);

        BigDecimal currentValue = holdingService.calculateCurrentValue(portfolioId);

        return ResponseEntity.ok(currentValue);
    }

    /**
     * Calculate unrealized P&L
     * GET /api/portfolios/{portfolioId}/holdings/unrealized-pnl
     */
    @GetMapping("/unrealized-pnl")
    @Operation(summary = "Calculate unrealized P&L", description = "Calculate total unrealized profit/loss")
    public ResponseEntity<BigDecimal> getUnrealizedPnL(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId) {
        log.info("API: Calculating unrealized P&L for portfolio: {}", portfolioId);

        BigDecimal unrealizedPnL = holdingService.calculateUnrealizedPnL(portfolioId);

        return ResponseEntity.ok(unrealizedPnL);
    }

    /**
     * Delete a holding
     * DELETE /api/portfolios/{portfolioId}/holdings/{holdingId}
     */
    @DeleteMapping("/{holdingId}")
    @Operation(summary = "Delete holding", description = "Delete a holding from portfolio")
    public ResponseEntity<Void> deleteHolding(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Holding ID")
            @PathVariable Long holdingId) {
        log.warn("API: Deleting holding ID: {} from portfolio: {}", holdingId, portfolioId);

        holdingService.deleteHolding(holdingId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Delete zero quantity holdings
     * DELETE /api/portfolios/{portfolioId}/holdings/cleanup
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "Cleanup zero holdings", description = "Delete all holdings with zero quantity")
    public ResponseEntity<Integer> deleteZeroQuantityHoldings(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId) {
        log.info("API: Cleaning up zero quantity holdings for portfolio: {}", portfolioId);

        int deletedCount = holdingService.deleteZeroQuantityHoldings(portfolioId);

        return ResponseEntity.ok(deletedCount);
    }
}
