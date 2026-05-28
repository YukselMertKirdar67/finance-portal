package com.financeportal.backend.Comparison;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/comparison")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Comparison API", description = "Enstrüman karşılaştırma")
public class ComparisonController {

    private final ComparisonService comparisonService;

    /**
     * İki enstrümanı belirtilen zaman dilimine göre karşılaştırır.
     * Anlık fiyatlar, tarihsel veriler ve performans metrikleri döner.
     */
    @GetMapping
    @Operation(summary = "İki enstrümanı karşılaştır")
    public ResponseEntity<ComparisonDTO> compareInstruments(
            @RequestParam Long id1,
            @RequestParam Long id2,
            @RequestParam(defaultValue = "1A") String period
    ) {
        log.info("Comparing instruments {} vs {} for period: {}", id1, id2, period);
        ComparisonDTO comparison = comparisonService.compareInstruments(id1, id2, period);
        return ResponseEntity.ok(comparison);
    }
}