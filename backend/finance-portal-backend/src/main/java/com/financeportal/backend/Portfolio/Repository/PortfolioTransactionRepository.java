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
    List<PortfolioTransaction> findByPortfolioIdAndDeletedFalseOrderByTransactionDateDesc(
            Long portfolioId
    );

    /**
     * Find all transactions by portfolio ID (paginated, sorted by date DESC)
     */
    Page<PortfolioTransaction> findByPortfolioIdAndDeletedFalseOrderByTransactionDateDesc(
            Long portfolioId, Pageable pageable
    );

    /**
     * Find transactions by portfolio ID with instrument (eager fetch)
     */
    @Query("SELECT t FROM PortfolioTransaction t " +
            "LEFT JOIN FETCH t.instrument " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.deleted = false " +
            "ORDER BY t.transactionDate DESC")
    List<PortfolioTransaction> findByPortfolioIdWithInstrument(@Param("portfolioId") Long portfolioId);

    /**
     * Find transactions by portfolio ID and transaction type
     */
    List<PortfolioTransaction> findByPortfolioIdAndTransactionTypeAndDeletedFalse(
            Long portfolioId,
            TransactionType transactionType
    );

    /**
     * Find transactions by portfolio ID and instrument ID
     */
    List<PortfolioTransaction> findByPortfolioIdAndInstrumentIdAndDeletedFalseOrderByTransactionDateDesc(
            Long portfolioId,
            Long instrumentId
    );

    /**
     * Find transactions by portfolio ID within date range
     */
    @Query("SELECT t FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.deleted = false " +
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
    long countByPortfolioIdAndDeletedFalse(Long portfolioId);

    /**
     * Count transactions by portfolio ID and type
     */
    long countByPortfolioIdAndTransactionTypeAndDeletedFalse(
            Long portfolioId,
            TransactionType transactionType
    );

    /**
     * Sum total buy amount for a portfolio
     */
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.transactionType = 'BUY' " +
            "AND t.deleted = false")
    BigDecimal sumBuyAmountByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Sum total sell amount for a portfolio
     */
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.transactionType = 'SELL' " +
            "AND t.deleted = false")
    BigDecimal sumSellAmountByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Sum total commission for a portfolio
     */
    @Query("SELECT COALESCE(SUM(t.commission), 0) FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.deleted = false")
    BigDecimal sumCommissionByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Sum total tax for a portfolio
     */
    @Query("SELECT COALESCE(SUM(t.tax), 0) FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.deleted = false")
    BigDecimal sumTaxByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Calculate realized P&L for a portfolio
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'SELL' THEN t.totalAmount ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'BUY' THEN t.totalAmount ELSE 0 END), 0) " +
            "FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.deleted = false")
    BigDecimal calculateRealizedPnL(@Param("portfolioId") Long portfolioId);

    /**
     * Find transactions by user ID (across all portfolios)
     */
    @Query("SELECT t FROM PortfolioTransaction t " +
            "WHERE t.portfolio.userId = :userId " +
            "AND t.deleted = false " +
            "ORDER BY t.transactionDate DESC")
    List<PortfolioTransaction> findByUserId(@Param("userId") String userId);

    /**
     * Find recent transactions (last N days)
     */
    @Query("SELECT t FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.deleted = false " +
            "AND t.transactionDate >= :sinceDate " +
            "ORDER BY t.transactionDate DESC")
    List<PortfolioTransaction> findRecentTransactions(
            @Param("portfolioId") Long portfolioId,
            @Param("sinceDate") LocalDateTime sinceDate
    );

    /**
     * Find transactions by instrument ID across all portfolios
     */
    List<PortfolioTransaction> findByInstrumentIdAndDeletedFalseOrderByTransactionDateDesc(
            Long instrumentId
    );

    /**
     * Get transaction statistics for a portfolio
     */
    @Query("SELECT " +
            "COUNT(t), " +
            "SUM(CASE WHEN t.transactionType = 'BUY' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN t.transactionType = 'SELL' THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(t.totalAmount), 0) " +
            "FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.deleted = false")
    Object[] getTransactionStatistics(@Param("portfolioId") Long portfolioId);

    /**
     * Delete transactions by portfolio ID (hard delete - admin only)
     */
    void deleteByPortfolioId(Long portfolioId);

    /**
     * Delete transactions older than a specific date
     */
    @Query("DELETE FROM PortfolioTransaction t " +
            "WHERE t.portfolio.id = :portfolioId " +
            "AND t.transactionDate < :beforeDate")
    void deleteOldTransactions(
            @Param("portfolioId") Long portfolioId,
            @Param("beforeDate") LocalDateTime beforeDate
    );
}