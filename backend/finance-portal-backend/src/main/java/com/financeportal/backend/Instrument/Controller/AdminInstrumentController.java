package com.financeportal.backend.Instrument.Controller;

import com.financeportal.backend.Instrument.DTO.InstrumentUpdateStatusDTO;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Repository.PriceHistoryRepository;
import com.financeportal.backend.Instrument.Service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/admin/instruments")
@RequiredArgsConstructor
@Tag(name = "Admin - Enstrüman Yönetimi", description = "Admin fiyat güncelleme ve enstrüman yönetimi endpoint'leri")

public class AdminInstrumentController {

    private final TcmbService tcmbService;
    private final TcmbEvdsService tcmbEvdsService;
    private final YahooFinanceService yahooFinanceService;
    private final ViopService viopService;
    private final InstrumentRepository instrumentRepository;

    private InstrumentUpdateStatusDTO lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
            .updating(false)
            .message("Henüz güncelleme yapılmadı")
            .build();

    // ========== STATUS ==========
    /**
     * Güncelleme durumunu döner.
     */
    @Operation(summary = "Güncelleme durumunu getir")
    @GetMapping("/update-status")
    public ResponseEntity<InstrumentUpdateStatusDTO> getUpdateStatus() {
        return ResponseEntity.ok(lastUpdateStatus);
    }

    // ========== TCMB ==========

    /**
     * TCMB'den günlük döviz kurlarını günceller.
     */
    @Operation(summary = "TCMB döviz kurlarını güncelle", description = "TCMB'den günlük döviz kurlarını çeker ve kaydeder")
    @PostMapping("/update-tcmb")
    public ResponseEntity<?> updateTcmbRates() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("TCMB kurları güncelleniyor...").build();
        try {
            int updated = tcmbService.fetchDailyRates().size();
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated).tcmbUpdated(updated)
                    .message("TCMB kurları güncellendi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "TCMB döviz kurları güncellendi",
                    "updatedCount", updated));
        } catch (Exception e) {
            log.error("❌ TCMB update failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("TCMB güncellemesi başarısız: " + e.getMessage()).build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "TCMB güncellemesi başarısız"));
        }
    }

    /**
     * TCMB arşivinden belirtilen gün sayısı kadar döviz geçmiş verisi çeker.
     * Varsayılan olarak son 365 günü çeker.
     */
    @Operation(summary = "Döviz geçmiş verisi çek", description = "TCMB arşivinden belirtilen gün sayısı kadar geçmiş veri çeker")
    @PostMapping("/fetch-forex-historical")
    public ResponseEntity<?> fetchForexHistoricalData(
            @RequestParam(defaultValue = "365") int days) {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("Döviz geçmiş verileri çekiliyor...").build();
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);
            tcmbService.fetchHistoricalRates(startDate, endDate);
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .message("Döviz geçmiş verileri çekildi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Döviz geçmiş verileri çekildi",
                    "days", days));
        } catch (Exception e) {
            log.error("❌ Forex historical failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("Geçmiş veri çekme başarısız").build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * TCMB EVDS'den Türkiye tahvil ve faiz oranlarını günceller.
     */
    @Operation(summary = "TR tahvil ve faiz oranlarını güncelle", description = "TCMB EVDS'den Türkiye tahvil ve faiz oranlarını günceller")
    @PostMapping("/update-tr-bonds")
    public ResponseEntity<?> updateTrBondYields() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("TR Tahvil/Faiz güncelleniyor...").build();
        try {
            int updated = tcmbEvdsService.fetchBondYields().size();
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated).bondsUpdated(updated)
                    .message("TR Tahvil/Faiz oranları güncellendi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "TR Tahvil/Faiz oranları güncellendi",
                    "updatedCount", updated));
        } catch (Exception e) {
            log.error("❌ TR Bonds update failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("TR Tahvil güncellemesi başarısız").build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * EVDS'den TR tahvil geçmiş verilerini çeker.
     */
    @Operation(summary = "TR tahvil geçmiş verisi çek", description = "EVDS'den TR tahvil geçmiş verilerini çeker")
    @PostMapping("/fetch-tr-bonds-historical")
    public ResponseEntity<?> fetchTrBondsHistorical(
            @RequestParam(defaultValue = "365") int days) {
        try {
            LocalDate endDate   = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);
            tcmbEvdsService.fetchBondYieldsHistorical(startDate, endDate);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "TR Tahvil geçmiş verileri çekildi",
                    "days", days));
        } catch (Exception e) {
            log.error("❌ TR Bonds historical failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== YAHOO FINANCE ==========

    /**
     * Yahoo Finance'den ABD tahvil ve hazine bonosu faiz oranlarını günceller.
     */
    @Operation(summary = "ABD tahvil faiz oranlarını güncelle", description = "Yahoo Finance'den ABD tahvil ve hazine bonosu faiz oranlarını günceller")
    @PostMapping("/update-bonds")
    public ResponseEntity<?> updateBondYields() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("Tahvil/Bono güncelleniyor...").build();
        try {
            int updated = yahooFinanceService.updateBonds();
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated).bondsUpdated(updated)
                    .message("Tahvil/Bono getiri oranları güncellendi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tahvil/Bono getiri oranları güncellendi",
                    "updatedCount", updated,
                    "note", "ABD 10Y, 30Y, 5Y tahvil ve 3 aylık hazine bonosu"));
        } catch (Exception e) {
            log.error("❌ Bonds update failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("Tahvil güncellemesi başarısız: " + e.getMessage()).build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Tahvil güncellemesi başarısız"));
        }
    }

    /**
     * Yahoo Finance'den ABD hisse senetlerini günceller.
     */
    @Operation(summary = "ABD hisse senetlerini güncelle")
    @PostMapping("/update-us-stocks")
    public ResponseEntity<?> updateUsStocks() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("ABD hisseleri güncelleniyor...").build();
        try {
            int updated = yahooFinanceService.updateUsStocks();
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated).message("ABD hisseleri güncellendi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ABD hisse senetleri güncellendi",
                    "updatedCount", updated));
        } catch (Exception e) {
            log.error("❌ US stocks update failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("ABD hisse güncellemesi başarısız: " + e.getMessage()).build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "ABD hisse güncellemesi başarısız"));
        }
    }

    /**
     * Yahoo Finance'den BIST hisse senetlerini günceller.
     */
    @Operation(summary = "BIST hisse senetlerini güncelle")
    @PostMapping("/update-bist")
    public ResponseEntity<?> updateBistStocks() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("BIST hisseleri güncelleniyor...").build();
        try {
            int updated = yahooFinanceService.updateBistStocks();
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated).bistUpdated(updated)
                    .message("BIST hisseleri güncellendi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "BIST hisseleri güncellendi",
                    "updatedCount", updated));
        } catch (Exception e) {
            log.error("❌ BIST update failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("BIST güncellemesi başarısız: " + e.getMessage()).build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "BIST güncellemesi başarısız"));
        }
    }

    /**
     * Yahoo Finance'den kripto para fiyatlarını günceller.
     */
    @Operation(summary = "Kripto para fiyatlarını güncelle")
    @PostMapping("/update-crypto")
    public ResponseEntity<?> updateCryptos() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("Kripto paralar güncelleniyor...").build();
        try {
            int updated = yahooFinanceService.updateCryptos();
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated).message("Kripto paralar güncellendi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Kripto paralar güncellendi",
                    "updatedCount", updated));
        } catch (Exception e) {
            log.error("❌ Crypto update failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("Kripto güncellemesi başarısız: " + e.getMessage()).build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Kripto güncellemesi başarısız"));
        }
    }

    /**
     * Yahoo Finance'den kıymetli metal fiyatlarını günceller.
     */
    @Operation(summary = "Kıymetli metal fiyatlarını güncelle")
    @PostMapping("/update-precious")
    public ResponseEntity<?> updatePreciousMetals() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("Kıymetli metaller güncelleniyor...").build();
        try {
            int updated = yahooFinanceService.updatePreciousMetals();
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated).preciousUpdated(updated)
                    .message("Kıymetli metaller güncellendi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Kıymetli metaller güncellendi",
                    "updatedCount", updated));
        } catch (Exception e) {
            log.error("❌ Precious metals update failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("Kıymetli metal güncellemesi başarısız: " + e.getMessage()).build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Kıymetli metal güncellemesi başarısız"));
        }
    }

    /**
     * Yahoo Finance'den ETF fiyatlarını günceller.
     */
    @Operation(summary = "ETF fiyatlarını güncelle")
    @PostMapping("/update-etfs")
    public ResponseEntity<?> updateEtfs() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("ETF'ler güncelleniyor...").build();
        try {
            int updated = yahooFinanceService.updateEtfs();
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(updated)
                    .message("ETF'ler güncellendi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ETF'ler güncellendi",
                    "updatedCount", updated));
        } catch (Exception e) {
            log.error("❌ ETF update failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("ETF güncellemesi başarısız").build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Belirtilen Yahoo Finance sembolü için anlık fiyatı günceller.
     */
    @Operation(summary = "Belirtilen sembolü güncelle", description = "Yahoo Finance sembolü için anlık fiyatı günceller")
    @PostMapping("/update-symbol/{yahooSymbol}/{dbSymbol}")
    public ResponseEntity<?> updateSymbol(@PathVariable String yahooSymbol,
                                          @PathVariable String dbSymbol) {
        InstrumentPrice price = yahooFinanceService.fetchQuote(yahooSymbol, dbSymbol);
        if (price == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Sembol için veri çekilemedi: " + yahooSymbol));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sembol güncellendi: " + dbSymbol,
                "price", price.getCurrentPrice()));
    }

    /**
     * Belirtilen enstrüman için geçmiş fiyat verisi çeker.
     */
    @Operation(summary = "Enstrüman geçmiş verisi çek", description = "Belirtilen enstrüman için geçmiş fiyat verisi çeker")
    @PostMapping("/fetch-historical/{yahooSymbol}/{dbSymbol}")
    public ResponseEntity<?> fetchHistoricalData(
            @PathVariable String yahooSymbol,
            @PathVariable String dbSymbol,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam(defaultValue = "1y") String range) {
        try {
            int count = yahooFinanceService.fetchHistoricalData(
                    yahooSymbol, dbSymbol, interval, range).size();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Geçmiş veri çekildi: " + dbSymbol,
                    "recordCount", count));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Geçmiş veri çekilemedi: " + e.getMessage()));
        }
    }

    /**
     * Tüm enstrümanlar için 1 yıllık geçmiş fiyat verisi çeker.
     */
    @Operation(summary = "Tüm enstrümanların geçmiş verisini çek", description = "Tüm enstrümanlar için 1 yıllık geçmiş fiyat verisi çeker")
    @PostMapping("/fetch-all-historical")
    public ResponseEntity<?> fetchAllHistoricalData() {
        try {
            yahooFinanceService.fetchAllHistoricalData();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tüm enstrümanlar için geçmiş veri çekildi"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Geçmiş veri çekilemedi: " + e.getMessage()));
        }
    }


    /**
     * VİOP fiyatlarını çeker.
     */
    @Operation(summary = "VİOP fiyatlarını güncelle")
    @PostMapping("/update-viop")
    public ResponseEntity<?> updateViop() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("VİOP fiyatları güncelleniyor...").build();
        try {
            viopService.fetchViopPrices();
            int count = instrumentRepository.findByExchangeAndActiveTrue("VIOP").size();
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .message("VİOP fiyatları güncellendi").build();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "VİOP fiyatları güncellendi",
                    "updatedCount", count));
        } catch (Exception e) {
            log.error("❌ VIOP update failed: {}", e.getMessage());
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("VİOP güncellemesi başarısız: " + e.getMessage()).build();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Geçmiş VİOP fiyatlarını çeker.
     */
    @Operation(summary = "VİOP tarihsel verilerini çek")
    @PostMapping("/fetch-viop-historical")
    public ResponseEntity<?> fetchViopHistorical() {
        try {
            viopService.fetchViopHistoricalData();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "VİOP tarihsel veriler çekildi"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }


    /**
     * Tüm enstrümanların fiyatlarını tek seferde günceller.
     * TCMB, Yahoo Finance, İş Yatırım verilerini kapsar.
     */
    @Operation(summary = "Tüm fiyatları güncelle", description = "TCMB, Yahoo Finance ve tahvil verilerini tek seferde günceller")
    @PostMapping("/update-all")
    public ResponseEntity<?> updateAllPrices() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("Tüm fiyatlar güncelleniyor...").build();
        try {
            int tcmbUpdated    = tcmbService.fetchDailyRates().size();
            int yahooUpdated   = yahooFinanceService.updateAll();
            int bondsUpdated = yahooFinanceService.updateBonds();
            int evdsUpdated = tcmbEvdsService.fetchBondYields().size();
            viopService.fetchViopPrices();
            int totalUpdated = tcmbUpdated + bondsUpdated + yahooUpdated + evdsUpdated;

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(totalUpdated)
                    .tcmbUpdated(tcmbUpdated)
                    .bondsUpdated(bondsUpdated)
                    .yahooUpdated(yahooUpdated)
                    .evdsUpdated(evdsUpdated)
                    .message("Tüm fiyatlar başarıyla güncellendi").build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tüm fiyatlar güncellendi",
                    "tcmbUpdated", tcmbUpdated,
                    "bondsUpdated", bondsUpdated,
                    "yahooUpdated", yahooUpdated,
                    "evdsUpdated", evdsUpdated,
                    "viopUpdated", "güncellendi",
                    "totalUpdated", totalUpdated));
        } catch (Exception e) {
            log.error("❌ Update all failed: {}", e.getMessage(), e);
            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).message("Güncelleme başarısız: " + e.getMessage()).build();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Güncelleme başarısız: " + e.getMessage()));
        }
    }


    /**
     * Kullanılan API kaynaklarının istatistiklerini döner.
     */
    @Operation(summary = "API istatistiklerini getir", description = "Kullanılan API kaynaklarının istatistiklerini döner")
    @GetMapping("/stats")
    public ResponseEntity<?> getApiStats() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "apis", Map.of(
                        "tcmb",         Map.of("limit", "Sınırsız", "usage", "Döviz kurları"),
                        "tcmbEvds",     Map.of("limit", "Sınırsız", "usage", "Tahvil/Bono"),
                        "yahooFinance", Map.of("limit", "Sınırsız", "usage", "ABD Hisse + BIST + Kripto + Emtia"),
                        "isYatirimViop", Map.of("limit", "Sınırsız", "usage", "VİOP Vadeli İşlemler")
                )));
    }
}