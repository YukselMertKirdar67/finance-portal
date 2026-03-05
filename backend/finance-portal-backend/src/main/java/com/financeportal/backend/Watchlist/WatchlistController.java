package com.financeportal.backend.Watchlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
@Tag(name = "Watchlist API", description = "Takip listesi yönetimi")
public class WatchlistController {

    private final WatchlistServiceImpl watchlistServiceImpl;

    @Qualifier("objectMapper")  // ✅ EKLE - Primary ObjectMapper
    private final ObjectMapper cleanMapper;

    /**
     * Watchlist'i getir (Paginated)
     */
    @GetMapping
    @Operation(summary = "Takip listesini getir")
    public ResponseEntity<String> getWatchlist(  // ✅ String döndür
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size
    ) throws Exception {  // ✅ Exception ekle
        WatchlistPageDTO result = watchlistServiceImpl.getWatchlist(page, size);

        // ✅ @class field'larını temizle
        String cleanJson = cleanMapper.writeValueAsString(result);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(cleanJson);
    }

    @PostMapping("/{instrumentId}")
    @Operation(summary = "Takip listesine ekle")
    public ResponseEntity<WatchlistDTO.WatchlistResponse> addToWatchlist(
            @PathVariable Long instrumentId
    ) {
        return ResponseEntity.ok(watchlistServiceImpl.addToWatchlist(instrumentId));
    }

    @DeleteMapping("/{instrumentId}")
    @Operation(summary = "Takip listesinden çıkar")
    public ResponseEntity<WatchlistDTO.WatchlistResponse> removeFromWatchlist(
            @PathVariable Long instrumentId
    ) {
        return ResponseEntity.ok(watchlistServiceImpl.removeFromWatchlist(instrumentId));
    }

    @GetMapping("/check/{instrumentId}")
    @Operation(summary = "Takip listesinde mi kontrol et")
    public ResponseEntity<Boolean> isInWatchlist(@PathVariable Long instrumentId) {
        return ResponseEntity.ok(watchlistServiceImpl.isInWatchlist(instrumentId));
    }
}