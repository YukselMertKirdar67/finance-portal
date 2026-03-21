package com.financeportal.backend.Instrument.Controller;

import com.financeportal.backend.Instrument.DTO.InstrumentUpdateStatusDTO;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/instruments")
@RequiredArgsConstructor
public class AdminInstrumentController {

    private final TcmbService tcmbService;
    private final FinnhubService finnhubService;
    private final TwelveDataService twelveDataService;
    private final AlphaVantageService alphaVantageService;
    private final TcmbEvdsService tcmbEvdsService;

    // Global update status
    private InstrumentUpdateStatusDTO lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
            .updating(false)
            .message("Henüz güncelleme yapılmadı")
            .build();

    /**
     * Son güncelleme durumunu getir
     */
    @GetMapping("/update-status")
    public ResponseEntity<InstrumentUpdateStatusDTO> getUpdateStatus() {
        return ResponseEntity.ok(lastUpdateStatus);
    }

    /**
     * TCMB kurlarını manuel güncelle
     */
    @PostMapping("/update-tcmb")
    public ResponseEntity<?> updateTcmbRates() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true)
                .message("TCMB kurları güncelleniyor...")
                .build();

        try {
            int updated = tcmbService.fetchDailyRates().size();

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated)
                    .tcmbUpdated(updated)
                    .message("TCMB kurları güncellendi")
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "TCMB döviz kurları güncellendi",
                    "updatedCount", updated
            ));

        } catch (Exception e) {
            log.error("❌ TCMB update failed: {}", e.getMessage());

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .message("TCMB güncellemesi başarısız: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "TCMB güncellemesi başarısız"
            ));
        }
    }

    /**
     * Finnhub API - Tüm enstrümanları güncelle
     */
    @PostMapping("/update-finnhub")
    public ResponseEntity<?> updateFinnhub() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true)
                .message("Finnhub güncellemesi devam ediyor...")
                .build();

        try {
            int updated = finnhubService.updatePriorityInstruments();

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated)
                    .finnhubUpdated(updated)
                    .message("Finnhub fiyatları güncellendi")
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Finnhub fiyatları güncellendi",
                    "updatedCount", updated
            ));

        } catch (Exception e) {
            log.error("❌ Finnhub update failed: {}", e.getMessage());

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .message("Finnhub güncellemesi başarısız: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Finnhub güncellemesi başarısız"
            ));
        }
    }

    /**
     * Finnhub API - Öncelikli enstrümanları güncelle (hızlı)
     */
    @PostMapping("/update-priority")
    public ResponseEntity<?> updatePriorityInstruments() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true)
                .message("Öncelikli enstrümanlar güncelleniyor...")
                .build();

        try {
            int updated = finnhubService.updatePriorityInstruments();

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated)
                    .finnhubUpdated(updated)
                    .message("Öncelikli enstrümanlar güncellendi")
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Öncelikli enstrümanlar güncellendi (BIST + ABD + Kripto)",
                    "updatedCount", updated
            ));

        } catch (Exception e) {
            log.error("❌ Priority update failed: {}", e.getMessage());

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .message("Öncelikli güncelleme başarısız: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Öncelikli güncelleme başarısız"
            ));
        }
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
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true)
                .message("ABD hisseleri güncelleniyor...")
                .build();

        try {
            int updated = finnhubService.updateUsStocks();

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated)
                    .finnhubUpdated(updated)
                    .message("ABD hisseleri güncellendi")
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ABD hisse senetleri güncellendi",
                    "updatedCount", updated
            ));

        } catch (Exception e) {
            log.error("❌ US stocks update failed: {}", e.getMessage());

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .message("ABD hisse güncellemesi başarısız: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "ABD hisse güncellemesi başarısız"
            ));
        }
    }

    /**
     * Finnhub API - Sadece kripto paraları güncelle
     */
    @PostMapping("/update-crypto")
    public ResponseEntity<?> updateCryptos() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true)
                .message("Kripto paralar güncelleniyor...")
                .build();

        try {
            int updated = finnhubService.updateCryptos();

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated)
                    .finnhubUpdated(updated)
                    .message("Kripto paralar güncellendi")
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Kripto paralar güncellendi",
                    "updatedCount", updated
            ));

        } catch (Exception e) {
            log.error("❌ Crypto update failed: {}", e.getMessage());

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .message("Kripto güncellemesi başarısız: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Kripto güncellemesi başarısız"
            ));
        }
    }

    /**
     * TwelveData API - BIST hisselerini güncelle
     */
    @PostMapping("/update-bist-twelvedata")
    public ResponseEntity<?> updateBistWithTwelveData() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true)
                .message("BIST hisseleri güncelleniyor...")
                .build();

        try {
            log.info("Starting BIST FULL update via TwelveData...");
            int updated = twelveDataService.updateBistStocksSample();

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated)
                    .bistUpdated(updated)
                    .message("BIST hisseleri güncellendi")
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "BIST hisseleri TwelveData ile güncellendi",
                    "updatedCount", updated,
                    "note", "İlk 3 hisse güncellendi (~24sn)"
            ));

        } catch (Exception e) {
            log.error("❌ BIST update failed: {}", e.getMessage());

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .message("BIST güncellemesi başarısız: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "BIST güncellemesi başarısız"
            ));
        }
    }

    /**
     * TwelveData API - Tek sembol güncelle
     */
    @PostMapping("/update-bist-symbol/{symbol}")
    public ResponseEntity<?> updateBistSymbol(@PathVariable String symbol) {
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

    /**
     * AlphaVantage - Kıymetli metaller
     */
    @PostMapping("/update-precious")
    public ResponseEntity<?> updatePreciousMetals() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true)
                .message("Kıymetli metaller güncelleniyor...")
                .build();

        try {
            log.info("Starting precious metals SAMPLE update via AlphaVantage...");
            int updated = alphaVantageService.updatePreciousMetalsSample();

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated)
                    .preciousUpdated(updated)
                    .message("Kıymetli metaller güncellendi")
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Kıymetli metaller güncellendi",
                    "updatedCount", updated,
                    "note", "İlk 2 metal güncellendi (~30sn)"
            ));

        } catch (Exception e) {
            log.error("❌ Precious metals update failed: {}", e.getMessage());

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .message("Kıymetli metal güncellemesi başarısız: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Kıymetli metal güncellemesi başarısız"
            ));
        }
    }


    /**
     * AlphaVantage - Tek kıymetli metal güncelle
     */
    @PostMapping("/update-precious/{symbol}")
    public ResponseEntity<?> updatePreciousSymbol(@PathVariable String symbol) {
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
     * TCMB EVDS - Tahvil/Bono getiri oranlarını güncelle
     */
    @PostMapping("/update-bonds")
    public ResponseEntity<?> updateBondYields() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true)
                .message("Tahvil/Bono güncellemesi devam ediyor...")
                .build();

        try {
            log.info("Starting bond yields update via TCMB EVDS...");
            List<InstrumentPrice> updated = tcmbEvdsService.fetchBondYields();

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated.size())
                    .bondsUpdated(updated.size())
                    .message("Tahvil/Bono getiri oranları güncellendi")
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tahvil/Bono getiri oranları güncellendi",
                    "updatedCount", updated.size(),
                    "note", "6 farklı vade (3A, 6A, 1Y, 2Y, 5Y, 10Y)"
            ));

        } catch (Exception e) {
            log.error("❌ Bonds update failed: {}", e.getMessage());

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .message("Tahvil güncellemesi başarısız: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Tahvil güncellemesi başarısız"
            ));
        }
    }

    /**
     * Tüm fiyatları güncelle
     */
    @PostMapping("/update-all")
    public ResponseEntity<?> updateAllPrices() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true)
                .message("Tüm fiyatlar güncelleniyor...")
                .build();

        try {
            int tcmbUpdated      = tcmbService.fetchDailyRates().size();
            int finnhubUpdated   = finnhubService.updatePriorityInstruments();
            int bistUpdated      = twelveDataService.updateBistStocksSample();
            int preciousUpdated  = alphaVantageService.updatePreciousMetalsSample();
            int bondsUpdated     = tcmbEvdsService.fetchBondYields().size();

            int totalUpdated = tcmbUpdated + finnhubUpdated + bistUpdated +
                    preciousUpdated + bondsUpdated;

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(totalUpdated)
                    .tcmbUpdated(tcmbUpdated)
                    .finnhubUpdated(finnhubUpdated)
                    .bistUpdated(bistUpdated)
                    .preciousUpdated(preciousUpdated)
                    .bondsUpdated(bondsUpdated)
                    .message("Tüm fiyatlar başarıyla güncellendi")
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tüm fiyatlar güncellendi",
                    "tcmbUpdated", tcmbUpdated,
                    "finnhubUpdated", finnhubUpdated,
                    "bistUpdated", bistUpdated,
                    "preciousUpdated", preciousUpdated,
                    "bondsUpdated", bondsUpdated,
                    "totalUpdated", totalUpdated
            ));

        } catch (Exception e) {
            log.error("❌ Update all failed: {}", e.getMessage(), e);

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false)
                    .message("Güncelleme başarısız: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Güncelleme başarısız: " + e.getMessage()
            ));
        }
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
                        "tcmbEvds",     Map.of("limit", "Sınırsız",      "usage", "Tahvil/Bono"),
                        "finnhub",      Map.of("limit", "60/dakika",     "usage", "ABD Hisse + Kripto"),
                        "twelveData",   Map.of("limit", "8/dakika",      "usage", "BIST Hisse"),
                        "alphaVantage", Map.of("limit", "25/gün",        "usage", "Altın/Gümüş")
                )
        ));
    }
}