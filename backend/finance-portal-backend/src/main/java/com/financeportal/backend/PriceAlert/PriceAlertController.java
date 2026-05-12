package com.financeportal.backend.PriceAlert;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/price-alerts")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Fiyat Alarmları", description = "Fiyat alarm yönetimi endpoint'leri")

public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    /**
     * Yeni fiyat alarmı oluşturur.
     */
    @Operation(summary = "Fiyat alarmı oluştur")
    @PostMapping
    public ResponseEntity<PriceAlertDTO> createAlert(
            @Valid @RequestBody CreatePriceAlertRequestDTO request) {
        log.info("Creating price alert for instrument: {}", request.getInstrumentId());
        return ResponseEntity.ok(priceAlertService.createAlert(request));
    }

    /**
     * Giriş yapmış kullanıcının tüm alarmlarını getirir.
     */
    @Operation(summary = "Tüm alarmları getir", description = "Giriş yapmış kullanıcının tüm alarmlarını listeler")
    @GetMapping
    public ResponseEntity<List<PriceAlertDTO>> getUserAlerts() {
        log.info("Fetching all user alerts");
        return ResponseEntity.ok(priceAlertService.getUserAlerts());
    }

    /**
     * Giriş yapmış kullanıcının aktif alarmlarını getirir.
     */
    @Operation(summary = "Aktif alarmları getir", description = "Giriş yapmış kullanıcının sadece aktif alarmlarını listeler")
    @GetMapping("/active")
    public ResponseEntity<List<PriceAlertDTO>> getActiveUserAlerts() {
        log.info("Fetching active user alerts");
        return ResponseEntity.ok(priceAlertService.getActiveUserAlerts());
    }

    /**
     * Belirtilen alarmı siler.
     */
    @Operation(summary = "Alarmı sil")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAlert(@PathVariable Long id) {
        log.info("Deleting price alert: {}", id);
        priceAlertService.deleteAlert(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Alarm silindi"));
    }
}
