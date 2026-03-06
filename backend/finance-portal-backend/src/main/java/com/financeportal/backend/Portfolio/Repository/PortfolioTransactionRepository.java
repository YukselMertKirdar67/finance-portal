package com.financeportal.backend.Portfolio.Repository;

import com.financeportal.backend.Portfolio.Entity.PortfolioTransaction;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, Long> {

    /**
     * Find all transactions by portfolio ID (sorted by date DESC)
     */
    List<PortfolioTransaction> findByPortfolioIdOrderByTransactionDateDesc(Long portfolioId);

    /**
     * Find all transactions by portfolio ID (paginated, sorted by date DESC)
     */
    Page<PortfolioTransaction> findByPortfolioIdOrderByTransactionDateDesc(Long portfolioId, Pageable pageable);

    /**
     * Find transactions by portfolio ID with instrument (eager fetch)
     */
    @Query("SELECT t FROM PortfolioTransaction t " +
            "LEFT JOIN FETCH t.instrument " +
            "WHERE t.portfolio.id = :portfolioId " +
            "ORDER BY t.transactionDate DESC")
    List<PortfolioTransaction> findByPortfolioIdWithInstrument(@Param("portfolioId") Long portfolioId);

    /**
     * Find transactions by portfolio ID and transaction type
     */
    List<PortfolioTransaction> findByPortfolioIdAndTransactionType(
            Long portfolioId,
            TransactionType transactionType
    );

    /**
     * Find transactions by portfolio ID and instrument ID
     */
    List<PortfolioTransaction> findByPortfolioIdAndInstrumentIdOrderByTransactionDateDesc(
            Long portfolioId,
            Long instrumentId
    );

    /**
     * Find transactions by portfolio ID within date range
     */
    @Query("SELECT t FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<PortfolioTransaction> findByPortfolioIdAndDateBetween(
            @Param("portfolioId") Long portfolioId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count transactions by portfolio ID
     */
    long countByPortfolioId(Long portfolioId);

    /**
     * Count transactions by portfolio ID and type
     */
    long countByPortfolioIdAndTransactionType(Long portfolioId, TransactionType transactionType);

    /**
     * Custom query: Sum total buy amount for a portfolio
     */
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.transactionType = 'BUY'")
    BigDecimal sumBuyAmountByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Custom query: Sum total sell amount for a portfolio
     */
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.transactionType = 'SELL'")
    BigDecimal sumSellAmountByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Custom query: Sum total commission for a portfolio
     */
    @Query("SELECT COALESCE(SUM(t.commission), 0) FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId")
    BigDecimal sumCommissionByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Custom query: Sum total tax for a portfolio
     */
    @Query("SELECT COALESCE(SUM(t.tax), 0) FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId")
    BigDecimal sumTaxByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Custom query: Calculate realized P&L for a portfolio
     * Realized P&L = Total Sell Amount - Total Buy Amount (for completed round-trips)
     * Note: This is a simplified calculation. Actual realized P&L requires FIFO/LIFO tracking.
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'SELL' THEN t.totalAmount ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'BUY' THEN t.totalAmount ELSE 0 END), 0) " +
            "FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId")
    BigDecimal calculateRealizedPnL(@Param("portfolioId") Long portfolioId);

    /**
     * Find transactions by user ID (across all portfolios)
     */
    @Query("SELECT t FROM PortfolioTransaction t " +
            "WHERE t.portfolio.userId = :userId " +
            "ORDER BY t.transactionDate DESC")
    List<PortfolioTransaction> findByUserId(@Param("userId") String userId);

    /**
     * Find recent transactions (last N days)
     */
    @Query("SELECT t FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.transactionDate >= :sinceDate " +
            "ORDER BY t.transactionDate DESC")
    List<PortfolioTransaction> findRecentTransactions(
            @Param("portfolioId") Long portfolioId,
            @Param("sinceDate") LocalDateTime sinceDate
    );

    /**
     * Find transactions by instrument ID across all portfolios
     */
    List<PortfolioTransaction> findByInstrumentIdOrderByTransactionDateDesc(Long instrumentId);

    /**
     * Custom query: Get transaction statistics for a portfolio
     * Returns: total count, buy count, sell count, total volume
     */
    @Query("SELECT " +
            "COUNT(t), " +
            "SUM(CASE WHEN t.transactionType = 'BUY' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN t.transactionType = 'SELL' THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(t.totalAmount), 0) " +
            "FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId")
    Object[] getTransactionStatistics(@Param("portfolioId") Long portfolioId);

    /**
     * Delete transactions by portfolio ID
     */
    void deleteByPortfolioId(Long portfolioId);

    /**
     * Delete transactions older than a specific date
     * (For archiving/cleanup purposes)
     */
    @Query("DELETE FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.transactionDate < :beforeDate")
    void deleteOldTransactions(
            @Param("portfolioId") Long portfolioId,
            @Param("beforeDate") LocalDateTime beforeDate
    );
}