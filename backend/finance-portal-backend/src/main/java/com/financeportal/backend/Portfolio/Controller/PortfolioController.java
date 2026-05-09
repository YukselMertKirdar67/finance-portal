package com.financeportal.backend.Portfolio.Controller;

import com.financeportal.backend.Portfolio.DTO.*;
import com.financeportal.backend.Portfolio.Service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
@Tag(name = "Portfolio", description = "Portfolio management APIs")
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Yeni portföy oluşturur
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
     * Giriş yapmış kullanıcının tüm portföylerini getirir
     */
    @GetMapping
    @Operation(summary = "Get user portfolios", description = "Get all portfolios for the current user")
    public ResponseEntity<List<PortfolioDTO>> getUserPortfolios() {
        log.info("API: Fetching user portfolios");

        List<PortfolioDTO> portfolios = portfolioService.getUserPortfolios();

        return ResponseEntity.ok(portfolios);
    }

    /**
     * Giriş yapmış kullanıcının portföylerini sayfalı olarak getirir.
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
     * Sadece aktif portföyleri getirir.
     */
    @GetMapping("/active")
    @Operation(summary = "Get active portfolios", description = "Get only active portfolios")
    public ResponseEntity<List<PortfolioDTO>> getActivePortfolios() {
        log.info("API: Fetching active portfolios");

        List<PortfolioDTO> portfolios = portfolioService.getActivePortfolios();

        return ResponseEntity.ok(portfolios);
    }

    /**
     * ID'ye göre portföy getirir.
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
     * Portföyün holding listesi dahil detay bilgilerini getirir.
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
     * Portföy adını, açıklamasını veya aktiflik durumunu günceller.
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
     * Portföyü soft delete ile pasif hale getirir.
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
     * Portföyü kalıcı olarak siler.
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
     * Portföyü aktif hale getirir.
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
     * Portföyü pasif hale getirir.
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
     * Dashboard için tüm portföylerin özetini getirir.
     * Toplam değer, kâr/zarar ve varlık dağılımı içerir.
     */
    @GetMapping("/summary")
    @Operation(summary = "Get portfolio summary", description = "Get summary of all portfolios for dashboard")
    public ResponseEntity<PortfolioSummaryDTO> getPortfolioSummary() {
        log.info("API: Fetching portfolio summary");

        PortfolioSummaryDTO summary = portfolioService.getPortfolioSummary();

        return ResponseEntity.ok(summary);
    }

    /**
     * Portföyün belirli gün sayısına ait performans geçmişini getirir.
     * Grafik için kullanılır.
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

        return ResponseEntity.ok(performance.getHistoricalData());
    }

    /**
     * Portföyleri isme göre arar.
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
     * Portföyleri türe göre filtreler.
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
     * Tüm portföylerin toplam güncel değerini TRY cinsinden hesaplar.
     */
    @GetMapping("/total-value")
    @Operation(summary = "Get total portfolio value", description = "Calculate total value across all portfolios")
    public ResponseEntity<BigDecimal> getTotalPortfolioValue() {
        log.info("API: Calculating total portfolio value");

        BigDecimal totalValue = portfolioService.calculateTotalPortfolioValue();

        return ResponseEntity.ok(totalValue);
    }

    /**
     * Tüm portföylerin toplam gerçekleşmemiş kâr/zararını hesaplar.
     */
    @GetMapping("/total-pnl")
    @Operation(summary = "Get total unrealized P&L", description = "Calculate total unrealized P&L across all portfolios")
    public ResponseEntity<BigDecimal> getTotalUnrealizedPnL() {
        log.info("API: Calculating total unrealized P&L");

        BigDecimal totalPnL = portfolioService.calculateTotalUnrealizedPnL();

        return ResponseEntity.ok(totalPnL);
    }
}
