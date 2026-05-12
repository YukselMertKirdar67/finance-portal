package com.financeportal.backend.Portfolio.Controller;

import com.financeportal.backend.Portfolio.Service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/portfolios")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Portfolio Admin", description = "Portfolio admin operasyonları (Sadece Admin)")
@SecurityRequirement(name = "bearer-auth")
public class PortfolioAdminController {

    private final PortfolioService portfolioService;

    /**
     * Tüm kullanıcıların portföylerini listeler.
     * Opsiyonel userId parametresiyle belirli kullanıcıya göre filtreler.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tüm portföyleri getir (admin)", description = "Tüm kullanıcılardan portföyleri getir")
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
     * Herhangi bir portföyü kalıcı olarak siler (admin yetkisi gerektirir).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Portföyü siler", description = "Herhangi bir portföyü kalıcı olarak siler (admin)")
    public ResponseEntity<Void> forceDeletePortfolio(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long id) {
        log.warn("API (Admin): Force deleting portfolio ID: {}", id);

        portfolioService.hardDeletePortfolio(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Sistem genelindeki portföy istatistiklerini getirir.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Portföy istatisliklerini getirir (admin)", description = "Sistemdeki portföylerin istatislikleri")
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        log.info("API (Admin): Fetching system-wide portfolio statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("message", "System statistics - Not implemented yet");

        return ResponseEntity.ok(stats);
    }
}
