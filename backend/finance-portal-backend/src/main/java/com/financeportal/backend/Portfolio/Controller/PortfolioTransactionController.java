package com.financeportal.backend.Portfolio.Controller;

import com.financeportal.backend.Portfolio.DTO.CreateTransactionRequestDTO;
import com.financeportal.backend.Portfolio.DTO.TransactionDTO;
import com.financeportal.backend.Portfolio.DTO.TransactionSummaryDTO;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import com.financeportal.backend.Portfolio.Service.PortfolioTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/transactions")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Portföy İşlemleri", description = "Portföy işlem yönetimi API'ları")
public class PortfolioTransactionController {

    private final PortfolioTransactionService transactionService;

    /**
     * Portföye alış veya satış işlemi ekler.
     */
    @PostMapping
    @Operation(summary = "İşlem oluştur", description = "Yeni bir ALIM veya SATIM işlemi oluşturur")
    public ResponseEntity<TransactionDTO> createTransaction(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Valid @RequestBody CreateTransactionRequestDTO request) {
        log.info("API: Creating {} transaction for portfolio: {}", request.getTransactionType(), portfolioId);

        TransactionDTO transaction = transactionService.createTransaction(portfolioId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Portföyün tüm işlem geçmişini sayfalı olarak getirir.
     */
    @GetMapping
    @Operation(summary = "İşlem geçmişini getir", description = "Bir portföye ait tüm işlemleri listeler (sayfalı)")
    public ResponseEntity<Page<TransactionDTO>> getTransactionHistory(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "50") int size) {

        log.info("API: Fetching transaction history for portfolio: {} (page: {}, size: {})",
                portfolioId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        Page<TransactionDTO> transactions = transactionService.getTransactionHistory(portfolioId, pageable);

        return ResponseEntity.ok(transactions);
    }

    /**
     * ID'ye göre işlem getirir.
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "ID ile işlem getir", description = "Belirli bir işlemin detaylarını getirir")
    public ResponseEntity<TransactionDTO> getTransactionById(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Transaction ID")
            @PathVariable Long transactionId) {
        log.info("API: Fetching transaction ID: {} for portfolio: {}", transactionId, portfolioId);

        TransactionDTO transaction = transactionService.getTransactionById(transactionId);

        return ResponseEntity.ok(transaction);
    }

    /**
     * İşlemleri türe göre filtreler (BUY/SELL).
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "Türe göre işlemleri getir", description = "ALIM veya SATIM türündeki işlemleri filtreleyerek getirir")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByType(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Transaction type (BUY or SELL)")
            @PathVariable TransactionType type) {
        log.info("API: Fetching {} transactions for portfolio: {}", type, portfolioId);

        List<TransactionDTO> transactions = transactionService.getTransactionsByType(portfolioId, type);

        return ResponseEntity.ok(transactions);
    }

    /**
     * Belirli bir enstrümana ait işlemleri getirir.
     */
    @GetMapping("/instrument/{instrumentId}")
    @Operation(summary = "Enstrümana göre işlemleri getir", description = "Belirli bir yatırım aracına (hisse, fon vb.) ait tüm işlemleri getirir")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByInstrument(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Instrument ID")
            @PathVariable Long instrumentId) {
        log.info("API: Fetching transactions for portfolio: {} and instrument: {}", portfolioId, instrumentId);

        List<TransactionDTO> transactions = transactionService.getTransactionsByInstrument(portfolioId, instrumentId);

        return ResponseEntity.ok(transactions);
    }

    /**
     * Belirtilen tarih aralığındaki işlemleri getirir.
     */
    @GetMapping("/date-range")
    @Operation(summary = "Tarih aralığına göre getir", description = "Belirli bir tarih aralığındaki işlemleri getirir")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateRange(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("API: Fetching transactions for portfolio: {} between {} and {}",
                portfolioId, startDate, endDate);

        List<TransactionDTO> transactions = transactionService.getTransactionsByDateRange(
                portfolioId, startDate, endDate);

        return ResponseEntity.ok(transactions);
    }

    /**
     * Son N gün içindeki işlemleri getirir.
     */
    @GetMapping("/recent")
    @Operation(summary = "Son işlemleri getir", description = "Son N güne ait işlemleri listeler")
    public ResponseEntity<List<TransactionDTO>> getRecentTransactions(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Number of days to look back")
            @RequestParam(defaultValue = "30") int days) {
        log.info("API: Fetching transactions for last {} days for portfolio: {}", days, portfolioId);

        List<TransactionDTO> transactions = transactionService.getRecentTransactions(portfolioId, days);

        return ResponseEntity.ok(transactions);
    }

    /**
     * Portföy için işlem özeti getirir.
     * Toplam alış/satış tutarı, komisyon, vergi ve kâr/zarar içerir.
     */
    @GetMapping("/summary")
    @Operation(summary = "İşlem özetini getir", description = "Tüm işlemler için özet istatistikleri getirir")
    public ResponseEntity<TransactionSummaryDTO> getTransactionSummary(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId) {
        log.info("API: Fetching transaction summary for portfolio: {}", portfolioId);

        TransactionSummaryDTO summary = transactionService.getTransactionSummary(portfolioId);

        return ResponseEntity.ok(summary);
    }

    /**
     * İşlemi soft delete ile siler.
     * Holding değişiklikleri geri alınmaz.
     */
    @DeleteMapping("/{transactionId}")
    @Operation(summary = "İşlemi sil", description = "Bir işlemi siler (UYARI: Bu işlem varlık bakiyelerini otomatik olarak geri almaz)")
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Parameter(description = "Transaction ID")
            @PathVariable Long transactionId) {
        log.warn("API: Deleting transaction ID: {} from portfolio: {}", transactionId, portfolioId);

        transactionService.deleteTransaction(transactionId);

        return ResponseEntity.noContent().build();
    }
}
