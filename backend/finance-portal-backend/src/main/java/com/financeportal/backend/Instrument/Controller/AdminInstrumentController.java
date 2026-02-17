package com.financeportal.backend.Instrument.Controller;

import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Service.AlphaVantageService;
import com.financeportal.backend.Instrument.Service.FinnhubService;
import com.financeportal.backend.Instrument.Service.TcmbService;
import com.financeportal.backend.Instrument.Service.TwelveDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/instruments")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')") // Şimdilik kapalı, sonra aç
public class AdminInstrumentController {

    private final TcmbService tcmbService;
    private final FinnhubService finnhubService;
    private final TwelveDataService twelveDataService;
    private final AlphaVantageService alphaVantageService;




    /**
     * TCMB kurlarını manuel güncelle
     */
    @PostMapping("/update-tcmb")
    public ResponseEntity<?> updateTcmbRates() {
        int updated = tcmbService.fetchDailyRates().size();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "TCMB döviz kurları güncellendi",
                "updatedCount", updated
        ));
    }

    /**
     * Finnhub API - Tüm enstrümanları güncelle
     */
    @PostMapping("/update-finnhub")
    public ResponseEntity<?> updateFinnhub() {
        int updated = finnhubService.updateAllInstruments();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Finnhub fiyatları güncellendi",
                "updatedCount", updated
        ));
    }

    /**
     * Finnhub API - Öncelikli enstrümanları güncelle (hızlı)
     */
    @PostMapping("/update-priority")
    public ResponseEntity<?> updatePriorityInstruments() {
        int updated = finnhubService.updatePriorityInstruments();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Öncelikli enstrümanlar güncellendi (BIST + ABD + Kripto)",
                "updatedCount", updated
        ));
    }

    /**
     * Finnhub API - Tek bir sembolü güncelle
     */
    @PostMapping("/update-symbol/{symbol}")
    public ResponseEntity<?> updateSymbol(@PathVariable String symbol) {
        var price = finnhubService.fetchQuote(symbol);

        if (price == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Sembol için veri çekilemedi: " + symbol
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sembol güncellendi: " + symbol,
                "price", price.getCurrentPrice()
        ));
    }

    /**
     * Finnhub API - Sadece ABD hisse senetlerini güncelle
     */
    @PostMapping("/update-us-stocks")
    public ResponseEntity<?> updateUsStocks() {
        int updated = finnhubService.updateUsStocks();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ABD hisse senetleri güncellendi",
                "updatedCount", updated
        ));
    }

    /**
     * Finnhub API - Sadece kripto paraları güncelle
     */
    @PostMapping("/update-crypto")
    public ResponseEntity<?> updateCryptos() {
        int updated = finnhubService.updateCryptos();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Kripto paralar güncellendi",
                "updatedCount", updated
        ));
    }

    /**
     * TwelveData API - BIST hisselerini güncelle
     */
    @PostMapping("/update-bist-twelvedata")
    public ResponseEntity<?> updateBistWithTwelveData() {
        log.info("Starting BIST update via TwelveData...");
        int updated = twelveDataService.updateBistStocks();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "BIST hisseleri TwelveData ile güncellendi",
                "updatedCount", updated,
                "note", "Rate limit: 8 istek/dk (her hisse ~8sn)"
        ));
    }

    /**
     * TwelveData API - Tek sembol güncelle
     */
    @PostMapping("/update-bist-symbol/{symbol}")
    public ResponseEntity<?> updateBistSymbol(@PathVariable String symbol) {
        // THYAO → THYAO:BIST formatına çevir
        String twelveDataSymbol = symbol.contains(":") ? symbol : symbol + ":BIST";

        InstrumentPrice price = twelveDataService.fetchQuote(twelveDataSymbol);

        if (price == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Sembol için veri çekilemedi: " + symbol
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "BIST sembolü güncellendi: " + symbol,
                "price", price.getCurrentPrice()
        ));
    }

    @PostMapping("/update-precious")
    public ResponseEntity<?> updatePreciousMetals() {
        log.info("Starting precious metals update via AlphaVantage...");
        int updated = alphaVantageService.updatePreciousMetals();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Kıymetli metaller güncellendi",
                "updatedCount", updated,
                "note", "Rate limit: 25 istek/gün (TTL: 1 saat)"
        ));
    }

    @PostMapping("/update-precious/{symbol}")
    public ResponseEntity<?> updatePreciousSymbol(@PathVariable String symbol) {
        // Örnek: XAU → XAUUSD / XAU/USD
        Map<String, String> symbolMap = Map.of(
                "XAU", "XAUUSD",
                "XAG", "XAGUSD",
                "XPT", "XPTUSD",
                "XPD", "XPDUSD"
        );

        String avSymbol = symbolMap.get(symbol.toUpperCase());
        if (avSymbol == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Geçersiz sembol. Kullanılabilir: XAU, XAG, XPT, XPD"
            ));
        }

        String dbSymbol = symbol.toUpperCase() + "/USD";
        InstrumentPrice price = alphaVantageService.fetchQuote(
                avSymbol, dbSymbol, symbol + " (Ons)", symbol, "oz"
        );

        if (price == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Veri çekilemedi: " + symbol
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Güncellendi: " + dbSymbol,
                "price", price.getCurrentPrice()
        ));
    }

    /**
     * Tüm fiyatları güncelle
     */
    @PostMapping("/update-all")
    public ResponseEntity<?> updateAllPrices() {
        int tcmbUpdated      = tcmbService.fetchDailyRates().size();
        int finnhubUpdated   = finnhubService.updatePriorityInstruments();
        int bistUpdated      = twelveDataService.updateBistStocks();
        int preciousUpdated  = alphaVantageService.updatePreciousMetals();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tüm fiyatlar güncellendi",
                "tcmbUpdated", tcmbUpdated,
                "finnhubUpdated", finnhubUpdated,
                "bistUpdated", bistUpdated,
                "preciousUpdated", preciousUpdated,
                "totalUpdated", tcmbUpdated + finnhubUpdated + bistUpdated + preciousUpdated
        ));
    }

    /**
     * API kullanım istatistikleri
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getApiStats() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "apis", Map.of(
                        "tcmb",         Map.of("limit", "Sınırsız",      "usage", "Döviz"),
                        "finnhub",      Map.of("limit", "60/dakika",     "usage", "ABD Hisse + Kripto"),
                        "twelveData",   Map.of("limit", "8/dakika",      "usage", "BIST Hisse"),
                        "alphaVantage", Map.of("limit", "25/gün",        "usage", "Altın/Gümüş")
                )
        ));
    }
}