package com.financeportal.backend.PriceAlert;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/price-alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @PostMapping
    public ResponseEntity<PriceAlertDTO> createAlert(
            @Valid @RequestBody CreatePriceAlertRequestDTO request) {
        return ResponseEntity.ok(priceAlertService.createAlert(request));
    }

    @GetMapping
    public ResponseEntity<List<PriceAlertDTO>> getUserAlerts() {
        return ResponseEntity.ok(priceAlertService.getUserAlerts());
    }

    @GetMapping("/active")
    public ResponseEntity<List<PriceAlertDTO>> getActiveUserAlerts() {
        return ResponseEntity.ok(priceAlertService.getActiveUserAlerts());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAlert(@PathVariable Long id) {
        priceAlertService.deleteAlert(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Alarm silindi"));
    }
}
