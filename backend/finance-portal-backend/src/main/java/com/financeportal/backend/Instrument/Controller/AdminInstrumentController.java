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
    private final TcmbEvdsService tcmbEvdsService;
    private final YahooFinanceService yahooFinanceService;

    private InstrumentUpdateStatusDTO lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
            .updating(false)
            .message("Henüz güncelleme yapılmadı")
            .build();

    // ========== STATUS ==========

    @GetMapping("/update-status")
    public ResponseEntity<InstrumentUpdateStatusDTO> getUpdateStatus() {
        return ResponseEntity.ok(lastUpdateStatus);
    }

    // ========== TCMB ==========

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

    // ========== TAHVIL/BONO ==========

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

    // ========== YAHOO FINANCE ==========

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

    // ========== TÜMÜNÜ GÜNCELLE ==========

    @PostMapping("/update-all")
    public ResponseEntity<?> updateAllPrices() {
        lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                .updating(true).message("Tüm fiyatlar güncelleniyor...").build();
        try {
            int tcmbUpdated    = tcmbService.fetchDailyRates().size();
            int yahooUpdated   = yahooFinanceService.updateAll();
            int bondsUpdated = yahooFinanceService.updateBonds();
            int totalUpdated   = tcmbUpdated + bondsUpdated + yahooUpdated;

            lastUpdateStatus = InstrumentUpdateStatusDTO.builder()
                    .updating(false).lastUpdateTime(LocalDateTime.now())
                    .totalUpdated(totalUpdated)
                    .tcmbUpdated(tcmbUpdated)
                    .bondsUpdated(bondsUpdated)
                    .yahooUpdated(yahooUpdated)
                    .message("Tüm fiyatlar başarıyla güncellendi").build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tüm fiyatlar güncellendi",
                    "tcmbUpdated", tcmbUpdated,
                    "bondsUpdated", bondsUpdated,
                    "yahooUpdated", yahooUpdated,
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

    // ========== STATS ==========

    @GetMapping("/stats")
    public ResponseEntity<?> getApiStats() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "apis", Map.of(
                        "tcmb",         Map.of("limit", "Sınırsız", "usage", "Döviz kurları"),
                        "tcmbEvds",     Map.of("limit", "Sınırsız", "usage", "Tahvil/Bono"),
                        "yahooFinance", Map.of("limit", "Sınırsız", "usage", "ABD Hisse + BIST + Kripto + Emtia")
                )));
    }
}