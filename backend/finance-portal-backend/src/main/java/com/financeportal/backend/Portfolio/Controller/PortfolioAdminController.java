package com.financeportal.backend.Portfolio.Controller;

import com.financeportal.backend.Portfolio.Service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/portfolios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolio Admin", description = "Portfolio admin operations (ADMIN only)")
@SecurityRequirement(name = "bearer-auth")
public class PortfolioAdminController {

    private final PortfolioService portfolioService;

    /**
     * Get all portfolios (admin view)
     * GET /api/admin/portfolios
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all portfolios (admin)", description = "Get all portfolios across all users")
    public ResponseEntity<Map<String, Object>> getAllPortfolios(
            @Parameter(description = "User ID filter (optional)")
            @RequestParam(required = false) String userId) {
        log.info("API (Admin): Fetching all portfolios. Filter userId: {}", userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin portfolio listing - Not implemented yet");
        response.put("userId", userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Force delete portfolio (admin)
     * DELETE /api/admin/portfolios/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force delete portfolio", description = "Permanently delete any portfolio (admin only)")
    public ResponseEntity<Void> forceDeletePortfolio(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id) {
        log.warn("API (Admin): Force deleting portfolio ID: {}", id);

        portfolioService.hardDeletePortfolio(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get portfolio statistics (admin)
     * GET /api/admin/portfolios/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get portfolio statistics (admin)", description = "Get system-wide portfolio statistics")
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        log.info("API (Admin): Fetching system-wide portfolio statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("message", "System statistics - Not implemented yet");

        return ResponseEntity.ok(stats);
    }
}
