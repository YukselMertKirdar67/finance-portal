package com.financeportal.backend.Instrument.Controller;

import com.financeportal.backend.Instrument.DTO.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Instrument.Service.InstrumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/instruments")
@RequiredArgsConstructor
@Tag(name = "Instruments API", description = "Finansal enstrümanlar yönetimi")
public class InstrumentController {

    private final InstrumentService instrumentService;

    /**
     * Tüm enstrümanları listele (sayfalı)
     */
    @GetMapping
    @Operation(summary = "Tüm enstrümanları listele")
    public ResponseEntity<Page<InstrumentResponseDTO>> getAllInstruments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(instrumentService.getAllInstruments(pageable));
    }

    /**
     * Tipe göre enstrümanları listele
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "Tipe göre enstrümanları listele")
    public ResponseEntity<Page<InstrumentResponseDTO>> getInstrumentsByType(
            @PathVariable InstrumentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(instrumentService.getInstrumentsByType(type, pageable));
    }

    /**
     * Enstrüman ara
     */
    @GetMapping("/search")
    @Operation(summary = "Enstrüman ara (sembol veya isim)")
    public ResponseEntity<Page<InstrumentResponseDTO>> searchInstruments(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(instrumentService.searchInstruments(query, pageable));
    }

    /**
     * ID ile enstrüman detayı
     */
    @GetMapping("/{id}")
    @Operation(summary = "Enstrüman detayı")
    public ResponseEntity<InstrumentResponseDTO> getInstrumentById(@PathVariable Long id) {
        return ResponseEntity.ok(instrumentService.getInstrumentById(id));
    }

    /**
     * Sembol ile enstrüman detayı (Query parameter - slash sorun değil!)
     */
    @GetMapping("/symbol")
    @Operation(summary = "Sembol ile enstrüman detayı (USD/TRY, BINANCE:BTCUSDT)")
    public ResponseEntity<InstrumentResponseDTO> getInstrumentBySymbol(
            @RequestParam String symbol
    ) {
        return ResponseEntity.ok(instrumentService.getInstrumentBySymbol(symbol));
    }

    /**
     * Enstrüman için anlık fiyat
     */
    @GetMapping("/{id}/price")
    @Operation(summary = "Anlık fiyat")
    public ResponseEntity<PriceDataDTO> getCurrentPrice(@PathVariable Long id) {
        return ResponseEntity.ok(instrumentService.getCurrentPrice(id));
    }

    /**
     * Enstrüman için geçmiş fiyatlar
     */
    @GetMapping("/{id}/history")
    @Operation(summary = "Geçmiş fiyatlar")
    public ResponseEntity<List<HistoricalPriceDTO>> getHistoricalPrices(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                instrumentService.getHistoricalPrices(id, startDate, endDate)
        );
    }
}