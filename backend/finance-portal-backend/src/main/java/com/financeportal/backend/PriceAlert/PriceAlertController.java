package com.financeportal.backend.PriceAlert;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/price-alerts")
@RequiredArgsConstructor
@Slf4j
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    /**
     * Yeni fiyat alarmı oluşturur.
     */

    @PostMapping
    public ResponseEntity<PriceAlertDTO> createAlert(
            @Valid @RequestBody CreatePriceAlertRequestDTO request) {
        log.info("Creating price alert for instrument: {}", request.getInstrumentId());
        return ResponseEntity.ok(priceAlertService.createAlert(request));
    }

    /**
     * Giriş yapmış kullanıcının tüm alarmlarını getirir.
     */

    @GetMapping
    public ResponseEntity<List<PriceAlertDTO>> getUserAlerts() {
        log.info("Fetching all user alerts");
        return ResponseEntity.ok(priceAlertService.getUserAlerts());
    }

    /**
     * Giriş yapmış kullanıcının aktif alarmlarını getirir.
     */

    @GetMapping("/active")
    public ResponseEntity<List<PriceAlertDTO>> getActiveUserAlerts() {
        log.info("Fetching active user alerts");
        return ResponseEntity.ok(priceAlertService.getActiveUserAlerts());
    }

    /**
     * Belirtilen alarmı siler.
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAlert(@PathVariable Long id) {
        log.info("Deleting price alert: {}", id);
        priceAlertService.deleteAlert(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Alarm silindi"));
    }
}
