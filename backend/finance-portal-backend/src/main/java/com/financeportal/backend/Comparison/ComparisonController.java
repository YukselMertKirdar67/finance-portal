package com.financeportal.backend.Comparison;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/comparison")
@RequiredArgsConstructor
@Tag(name = "Comparison API", description = "Enstrüman karşılaştırma")
public class ComparisonController {

    private final ComparisonService comparisonService;

    /**
     * İki enstrümanı karşılaştır
     */
    @GetMapping
    @Operation(summary = "İki enstrümanı karşılaştır")
    public ResponseEntity<ComparisonDTO> compareInstruments(
            @RequestParam Long id1,
            @RequestParam Long id2,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // Default: Son 30 gün
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        ComparisonDTO comparison = comparisonService.compareInstruments(
                id1, id2, startDate, endDate
        );

        return ResponseEntity.ok(comparison);
    }
}