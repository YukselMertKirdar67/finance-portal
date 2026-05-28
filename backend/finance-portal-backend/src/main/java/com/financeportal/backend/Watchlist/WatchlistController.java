package com.financeportal.backend.Watchlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Watchlist API", description = "Takip listesi yönetimi")
public class WatchlistController {

    private final WatchlistService watchlistService;

    @Qualifier("objectMapper")
    private final ObjectMapper cleanMapper;

    /**
     * Kullanıcının takip listesini sayfalı olarak getirir
     */
    @GetMapping
    @Operation(summary = "Takip listesini getir")
    public ResponseEntity<String> getWatchlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws Exception {
        log.info("Fetching watchlist page: {}, size: {}", page, size);
        WatchlistPageDTO result = watchlistService.getWatchlist(page, size);
        String cleanJson = cleanMapper.writeValueAsString(result);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(cleanJson);
    }

    /**
     * Enstrümanı takip listesine ekler
     */
    @PostMapping("/{instrumentId}")
    @Operation(summary = "Takip listesine ekle")
    public ResponseEntity<WatchlistDTO.WatchlistResponse> addToWatchlist(
            @PathVariable Long instrumentId
    ) {
        log.info("Adding instrument {} to watchlist", instrumentId);
        return ResponseEntity.ok(watchlistService.addToWatchlist(instrumentId));
    }

    /**
     * Enstrümanı takip listesinden çıkarır
     */
    @DeleteMapping("/{instrumentId}")
    @Operation(summary = "Takip listesinden çıkar")
    public ResponseEntity<WatchlistDTO.WatchlistResponse> removeFromWatchlist(
            @PathVariable Long instrumentId
    ) {
        log.info("Removing instrument {} from watchlist", instrumentId);
        return ResponseEntity.ok(watchlistService.removeFromWatchlist(instrumentId));
    }

    /**
     * Enstrümanın takip listesinde olup olmadığını kontrol eder
     */
    @GetMapping("/check/{instrumentId}")
    @Operation(summary = "Takip listesinde mi kontrol et")
    public ResponseEntity<Boolean> isInWatchlist(@PathVariable Long instrumentId) {
        log.info("Checking watchlist status for instrument: {}", instrumentId);
        return ResponseEntity.ok(watchlistService.isInWatchlist(instrumentId));
    }
}