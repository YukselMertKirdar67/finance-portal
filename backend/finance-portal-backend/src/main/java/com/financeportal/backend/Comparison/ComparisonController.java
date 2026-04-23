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
     * İki enstrümanı karşılaştır (Period bazlı)
     */
    @GetMapping
    @Operation(summary = "İki enstrümanı karşılaştır")
    public ResponseEntity<ComparisonDTO> compareInstruments(
            @RequestParam Long id1,
            @RequestParam Long id2,
            @RequestParam(defaultValue = "1A") String period
    ) {
        ComparisonDTO comparison = comparisonService.compareInstruments(id1, id2, period);
        return ResponseEntity.ok(comparison);
    }
}