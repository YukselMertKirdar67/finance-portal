package com.financeportal.backend.Portfolio.Controller;

import com.financeportal.backend.Portfolio.DTO.PortfolioDTO;
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
import java.util.List;
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
    @Operation(summary = "Tüm portföyleri getir (admin)")
    public ResponseEntity<List<PortfolioDTO>> getAllPortfolios(
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(portfolioService.getAllPortfoliosAdmin(userId));
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
    @Operation(summary = "Sistem istatistiklerini getir (admin)")
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        return ResponseEntity.ok(portfolioService.getSystemStatistics());
    }
}
