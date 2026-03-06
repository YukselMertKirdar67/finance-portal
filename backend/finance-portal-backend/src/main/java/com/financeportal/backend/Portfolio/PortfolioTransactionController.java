package com.financeportal.backend.Portfolio;

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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Tag(name = "Portfolio Transactions", description = "Portfolio transaction management APIs")
public class PortfolioTransactionController {

    private final PortfolioTransactionService transactionService;

    /**
     * Create a new transaction (BUY or SELL)
     * POST /api/portfolios/{portfolioId}/transactions
     */
    @PostMapping
    @Operation(summary = "Create transaction", description = "Create a new BUY or SELL transaction")
    public ResponseEntity<TransactionDTO> createTransaction(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId,
            @Valid @RequestBody CreateTransactionRequestDTO request) {
        log.info("API: Creating {} transaction for portfolio: {}", request.getTransactionType(), portfolioId);

        TransactionDTO transaction = transactionService.createTransaction(portfolioId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Get all transactions for a portfolio
     * GET /api/portfolios/{portfolioId}/transactions
     */
    @GetMapping
    @Operation(summary = "Get transaction history", description = "Get all transactions for a portfolio (paginated)")
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
     * Get transaction by ID
     * GET /api/portfolios/{portfolioId}/transactions/{transactionId}
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID", description = "Get details of a specific transaction")
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
     * Get transactions by type (BUY or SELL)
     * GET /api/portfolios/{portfolioId}/transactions/type/{type}
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "Get transactions by type", description = "Get BUY or SELL transactions")
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
     * Get transactions for a specific instrument
     * GET /api/portfolios/{portfolioId}/transactions/instrument/{instrumentId}
     */
    @GetMapping("/instrument/{instrumentId}")
    @Operation(summary = "Get transactions by instrument", description = "Get all transactions for a specific instrument")
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
     * Get transactions within date range
     * GET /api/portfolios/{portfolioId}/transactions/date-range
     */
    @GetMapping("/date-range")
    @Operation(summary = "Get transactions by date range", description = "Get transactions within a date range")
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
     * Get recent transactions (last N days)
     * GET /api/portfolios/{portfolioId}/transactions/recent
     */
    @GetMapping("/recent")
    @Operation(summary = "Get recent transactions", description = "Get transactions from the last N days")
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
     * Get transaction summary
     * GET /api/portfolios/{portfolioId}/transactions/summary
     */
    @GetMapping("/summary")
    @Operation(summary = "Get transaction summary", description = "Get summary statistics for all transactions")
    public ResponseEntity<TransactionSummaryDTO> getTransactionSummary(
            @Parameter(description = "Portfolio ID")
            @PathVariable Long portfolioId) {
        log.info("API: Fetching transaction summary for portfolio: {}", portfolioId);

        TransactionSummaryDTO summary = transactionService.getTransactionSummary(portfolioId);

        return ResponseEntity.ok(summary);
    }

    /**
     * Delete a transaction
     * DELETE /api/portfolios/{portfolioId}/transactions/{transactionId}
     */
    @DeleteMapping("/{transactionId}")
    @Operation(summary = "Delete transaction", description = "Delete a transaction (WARNING: Does not reverse holding changes)")
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
