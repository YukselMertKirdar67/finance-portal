package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Portfolio.DTO.CreateTransactionRequestDTO;
import com.financeportal.backend.Portfolio.DTO.TransactionDTO;
import com.financeportal.backend.Portfolio.DTO.TransactionSummaryDTO;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PortfolioTransactionService {

    /**
     * Create a new transaction (BUY or SELL)
     * This method handles:
     * - Creating the transaction record
     * - Updating or creating portfolio holding
     * - Recalculating average buy price (for BUY)
     * - Reducing quantity or removing holding (for SELL)
     *
     * @param portfolioId Portfolio ID
     * @param request Transaction request
     * @return Created transaction DTO
     */
    TransactionDTO createTransaction(Long portfolioId, CreateTransactionRequestDTO request);

    /**
     * Create a BUY transaction
     * Internal method called by createTransaction
     *
     * @param portfolioId Portfolio ID
     * @param request Transaction request
     * @return Created transaction DTO
     */
    TransactionDTO createBuyTransaction(Long portfolioId, CreateTransactionRequestDTO request);

    /**
     * Create a SELL transaction
     * Internal method called by createTransaction
     * Validates that user has sufficient quantity to sell
     *
     * @param portfolioId Portfolio ID
     * @param request Transaction request
     * @return Created transaction DTO
     */
    TransactionDTO createSellTransaction(Long portfolioId, CreateTransactionRequestDTO request);

    /**
     * Get transaction by ID
     *
     * @param transactionId Transaction ID
     * @return Transaction DTO
     */
    TransactionDTO getTransactionById(Long transactionId);

    /**
     * Get all transactions for a portfolio (sorted by date DESC)
     *
     * @param portfolioId Portfolio ID
     * @return List of transaction DTOs
     */
    List<TransactionDTO> getTransactionHistory(Long portfolioId);

    /**
     * Get transactions for a portfolio (paginated)
     *
     * @param portfolioId Portfolio ID
     * @param pageable Pagination parameters
     * @return Page of transaction DTOs
     */
    Page<TransactionDTO> getTransactionHistory(Long portfolioId, Pageable pageable);

    /**
     * Get transactions by type (BUY or SELL)
     *
     * @param portfolioId Portfolio ID
     * @param transactionType Transaction type
     * @return List of transactions
     */
    List<TransactionDTO> getTransactionsByType(Long portfolioId, TransactionType transactionType);

    /**
     * Get transactions for a specific instrument in a portfolio
     *
     * @param portfolioId Portfolio ID
     * @param instrumentId Instrument ID
     * @return List of transactions
     */
    List<TransactionDTO> getTransactionsByInstrument(Long portfolioId, Long instrumentId);

    /**
     * Get transactions within a date range
     *
     * @param portfolioId Portfolio ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of transactions
     */
    List<TransactionDTO> getTransactionsByDateRange(
            Long portfolioId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Get recent transactions (last N days)
     *
     * @param portfolioId Portfolio ID
     * @param days Number of days to look back
     * @return List of recent transactions
     */
    List<TransactionDTO> getRecentTransactions(Long portfolioId, int days);

    /**
     * Get transaction summary for a portfolio
     * Includes: total transactions, buy/sell counts, total amounts, commission, tax, etc.
     *
     * @param portfolioId Portfolio ID
     * @return Transaction summary DTO
     */
    TransactionSummaryDTO getTransactionSummary(Long portfolioId);

    /**
     * Calculate total buy amount for a portfolio
     *
     * @param portfolioId Portfolio ID
     * @return Total buy amount
     */
    BigDecimal calculateTotalBuyAmount(Long portfolioId);

    /**
     * Calculate total sell amount for a portfolio
     *
     * @param portfolioId Portfolio ID
     * @return Total sell amount
     */
    BigDecimal calculateTotalSellAmount(Long portfolioId);

    /**
     * Calculate total commission paid
     *
     * @param portfolioId Portfolio ID
     * @return Total commission
     */
    BigDecimal calculateTotalCommission(Long portfolioId);

    /**
     * Calculate total tax paid
     *
     * @param portfolioId Portfolio ID
     * @return Total tax
     */
    BigDecimal calculateTotalTax(Long portfolioId);

    /**
     * Calculate realized P&L
     * This is a simplified calculation: Total Sell - Total Buy
     * Note: Proper realized P&L requires FIFO/LIFO lot tracking
     *
     * @param portfolioId Portfolio ID
     * @return Realized P&L
     */
    BigDecimal calculateRealizedPnL(Long portfolioId);

    /**
     * Soft delete a transaction (sets deleted = true)
     * Transaction record is kept in database but hidden from user
     *
     * @param transactionId Transaction ID
     */
    void deleteTransaction(Long transactionId);

    /**
     * Count transactions for a portfolio
     *
     * @param portfolioId Portfolio ID
     * @return Transaction count
     */
    long countTransactions(Long portfolioId);

    /**
     * Count buy transactions
     *
     * @param portfolioId Portfolio ID
     * @return Buy transaction count
     */
    long countBuyTransactions(Long portfolioId);

    /**
     * Count sell transactions
     *
     * @param portfolioId Portfolio ID
     * @return Sell transaction count
     */
    long countSellTransactions(Long portfolioId);
}
