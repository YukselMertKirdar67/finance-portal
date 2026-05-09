package com.financeportal.backend.Portfolio.Controller;

import com.financeportal.backend.Portfolio.DTO.AssetAllocationDTO;
import com.financeportal.backend.Portfolio.DTO.HoldingDTO;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Service.PortfolioHoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/holdings")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Portfolio Holdings", description = "Portfolio holdings management APIs")
public class PortfolioHoldingController {

    private final PortfolioHoldingService holdingService;
    private final PortfolioRepository portfolioRepository;

    /**
     * Portföydeki tüm holdingleri güncel fiyatlarla getirir.
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
     * Portföydeki aktif holdingleri getirir (miktar > 0).
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
     * Portföydeki en yüksek değerli N holdingi getirir.
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
     * ID'ye göre holding getirir.
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
     * Portföydeki varlık dağılımını portföy currency'sine göre hesaplar.
     */
    @GetMapping("/asset-allocation")
    @Operation(summary = "Get asset allocation")
    public ResponseEntity<List<AssetAllocationDTO>> getAssetAllocation(
            @PathVariable Long portfolioId) {
        log.info("API: Fetching asset allocation for portfolio: {}", portfolioId);

        String currency = portfolioRepository.findById(portfolioId)
                .map(p -> p.getCurrency() != null ? p.getCurrency() : "TRY")
                .orElse("TRY");

        List<AssetAllocationDTO> allocation = holdingService.getAssetAllocation(portfolioId, currency);

        return ResponseEntity.ok(allocation);
    }

    /**
     * Portföydeki toplam yatırım tutarını hesaplar.
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
     * Portföydeki toplam güncel değeri hesaplar.
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
     * Portföydeki toplam gerçekleşmemiş kâr/zararı hesaplar.
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
     * Belirtilen holdingi portföyden siler.
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
     * Portföydeki sıfır miktarlı holdingleri temizler.
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
