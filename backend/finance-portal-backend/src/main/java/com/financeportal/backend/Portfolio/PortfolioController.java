package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Portfolio.DTO.*;
import com.financeportal.backend.Portfolio.Service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolio", description = "Portfolio management APIs")
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Create a new portfolio
     * POST /api/portfolios
     */
    @PostMapping
    @Operation(summary = "Create new portfolio", description = "Create a new portfolio for the current user")
    public ResponseEntity<PortfolioDTO> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequestDTO request) {
        log.info("API: Creating new portfolio: {}", request.getName());

        PortfolioDTO portfolio = portfolioService.createPortfolio(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(portfolio);
    }

    /**
     * Get all portfolios for current user
     * GET /api/portfolios
     */
    @GetMapping
    @Operation(summary = "Get user portfolios", description = "Get all portfolios for the current user")
    public ResponseEntity<List<PortfolioDTO>> getUserPortfolios() {
        log.info("API: Fetching user portfolios");

        List<PortfolioDTO> portfolios = portfolioService.getUserPortfolios();

        return ResponseEntity.ok(portfolios);
    }

    /**
     * Get portfolios with pagination
     * GET /api/portfolios/paginated
     */
    @GetMapping("/paginated")
    @Operation(summary = "Get user portfolios (paginated)", description = "Get portfolios with pagination")
    public ResponseEntity<Page<PortfolioDTO>> getUserPortfoliosPaginated(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("API: Fetching user portfolios (page: {}, size: {}, sort: {} {})",
                page, size, sortBy, sortDir);

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<PortfolioDTO> portfolios = portfolioService.getUserPortfolios(pageable);

        return ResponseEntity.ok(portfolios);
    }

    /**
     * Get active portfolios only
     * GET /api/portfolios/active
     */
    @GetMapping("/active")
    @Operation(summary = "Get active portfolios", description = "Get only active portfolios")
    public ResponseEntity<List<PortfolioDTO>> getActivePortfolios() {
        log.info("API: Fetching active portfolios");

        List<PortfolioDTO> portfolios = portfolioService.getActivePortfolios();

        return ResponseEntity.ok(portfolios);
    }

    /**
     * Get portfolio by ID
     * GET /api/portfolios/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get portfolio by ID", description = "Get basic portfolio information")
    public ResponseEntity<PortfolioDTO> getPortfolioById(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id) {
        log.info("API: Fetching portfolio by ID: {}", id);

        PortfolioDTO portfolio = portfolioService.getPortfolioById(id);

        return ResponseEntity.ok(portfolio);
    }

    /**
     * Get portfolio detail (with holdings)
     * GET /api/portfolios/{id}/detail
     */
    @GetMapping("/{id}/detail")
    @Operation(summary = "Get portfolio detail", description = "Get detailed portfolio information with holdings")
    public ResponseEntity<PortfolioDetailDTO> getPortfolioDetail(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id) {
        log.info("API: Fetching portfolio detail for ID: {}", id);

        PortfolioDetailDTO detail = portfolioService.getPortfolioDetail(id);

        return ResponseEntity.ok(detail);
    }

    /**
     * Update portfolio
     * PUT /api/portfolios/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update portfolio", description = "Update portfolio information")
    public ResponseEntity<PortfolioDTO> updatePortfolio(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id,
            @Valid @RequestBody UpdatePortfolioRequestDTO request) {
        log.info("API: Updating portfolio ID: {}", id);

        PortfolioDTO updated = portfolioService.updatePortfolio(id, request);

        return ResponseEntity.ok(updated);
    }

    /**
     * Soft delete portfolio
     * DELETE /api/portfolios/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete portfolio", description = "Soft delete portfolio (set active to false)")
    public ResponseEntity<Void> deletePortfolio(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id) {
        log.info("API: Deleting portfolio ID: {}", id);

        portfolioService.deletePortfolio(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Hard delete portfolio
     * DELETE /api/portfolios/{id}/hard
     */
    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Hard delete portfolio", description = "Permanently delete portfolio")
    public ResponseEntity<Void> hardDeletePortfolio(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id) {
        log.warn("API: Hard deleting portfolio ID: {}", id);

        portfolioService.hardDeletePortfolio(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Activate portfolio
     * PATCH /api/portfolios/{id}/activate
     */
    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate portfolio", description = "Set portfolio as active")
    public ResponseEntity<Void> activatePortfolio(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id) {
        log.info("API: Activating portfolio ID: {}", id);

        portfolioService.activatePortfolio(id);

        return ResponseEntity.ok().build();
    }

    /**
     * Deactivate portfolio
     * PATCH /api/portfolios/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate portfolio", description = "Set portfolio as inactive")
    public ResponseEntity<Void> deactivatePortfolio(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id) {
        log.info("API: Deactivating portfolio ID: {}", id);

        portfolioService.deactivatePortfolio(id);

        return ResponseEntity.ok().build();
    }

    /**
     * Get portfolio summary (dashboard)
     * GET /api/portfolios/summary
     */
    @GetMapping("/summary")
    @Operation(summary = "Get portfolio summary", description = "Get summary of all portfolios for dashboard")
    public ResponseEntity<PortfolioSummaryDTO> getPortfolioSummary() {
        log.info("API: Fetching portfolio summary");

        PortfolioSummaryDTO summary = portfolioService.getPortfolioSummary();

        return ResponseEntity.ok(summary);
    }

    /**
     * Get portfolio performance history (for chart)
     * GET /api/portfolios/{id}/performance?days=30
     */
    @GetMapping("/{id}/performance")
    @Operation(summary = "Get portfolio performance history", description = "Get historical performance data for chart")
    public ResponseEntity<List<PerformanceDataPointDTO>> getPortfolioPerformanceHistory(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id,
            @Parameter(description = "Number of days (default: 30)")
            @RequestParam(defaultValue = "30") int days) {

        log.info("API: Fetching portfolio performance history for ID: {} (last {} days)", id, days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        PortfolioPerformanceDTO performance = portfolioService.getPortfolioPerformance(id, startDate, endDate);

        // Return only historical data (array format for chart)
        return ResponseEntity.ok(performance.getHistoricalData());
    }

    /**
     * Search portfolios by name
     * GET /api/portfolios/search
     */
    @GetMapping("/search")
    @Operation(summary = "Search portfolios", description = "Search portfolios by name")
    public ResponseEntity<List<PortfolioDTO>> searchPortfolios(
            @Parameter(description = "Search term")
            @RequestParam String query) {
        log.info("API: Searching portfolios with query: {}", query);

        List<PortfolioDTO> portfolios = portfolioService.searchPortfoliosByName(query);

        return ResponseEntity.ok(portfolios);
    }

    /**
     * Get portfolios by type
     * GET /api/portfolios/type/{type}
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "Get portfolios by type", description = "Get portfolios filtered by type")
    public ResponseEntity<List<PortfolioDTO>> getPortfoliosByType(
            @Parameter(description = "Portfolio type (PERSONAL, BUSINESS, RETIREMENT, SAVINGS)")
            @PathVariable String type) {
        log.info("API: Fetching portfolios by type: {}", type);

        List<PortfolioDTO> portfolios = portfolioService.getPortfoliosByType(type);

        return ResponseEntity.ok(portfolios);
    }

    /**
     * Calculate total portfolio value (all portfolios)
     * GET /api/portfolios/total-value
     */
    @GetMapping("/total-value")
    @Operation(summary = "Get total portfolio value", description = "Calculate total value across all portfolios")
    public ResponseEntity<BigDecimal> getTotalPortfolioValue() {
        log.info("API: Calculating total portfolio value");

        BigDecimal totalValue = portfolioService.calculateTotalPortfolioValue();

        return ResponseEntity.ok(totalValue);
    }

    /**
     * Calculate total unrealized P&L (all portfolios)
     * GET /api/portfolios/total-pnl
     */
    @GetMapping("/total-pnl")
    @Operation(summary = "Get total unrealized P&L", description = "Calculate total unrealized P&L across all portfolios")
    public ResponseEntity<BigDecimal> getTotalUnrealizedPnL() {
        log.info("API: Calculating total unrealized P&L");

        BigDecimal totalPnL = portfolioService.calculateTotalUnrealizedPnL();

        return ResponseEntity.ok(totalPnL);
    }
}
