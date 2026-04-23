package com.financeportal.backend.Portfolio.Repository;

import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Entity.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {

    /**
     * Find all holdings by portfolio ID
     */
    List<PortfolioHolding> findByPortfolioId(Long portfolioId);

    /**
     * Find holdings by portfolio (eager fetch instrument)
     */
    @Query("SELECT h FROM PortfolioHolding h " +
            "LEFT JOIN FETCH h.instrument " +
            "WHERE h.portfolio.id = :portfolioId")
    List<PortfolioHolding> findByPortfolioIdWithInstrument(@Param("portfolioId") Long portfolioId);

    /**
     * Find holding by portfolio ID and instrument ID
     * (Used to check if instrument already exists in portfolio)
     */
    Optional<PortfolioHolding> findByPortfolioIdAndInstrumentId(Long portfolioId, Long instrumentId);

    /**
     * Find holding by portfolio and instrument (entity objects)
     */
    Optional<PortfolioHolding> findByPortfolioAndInstrument(Portfolio portfolio, BaseInstrument instrument);

    /**
     * Check if holding exists for portfolio and instrument
     */
    boolean existsByPortfolioIdAndInstrumentId(Long portfolioId, Long instrumentId);

    /**
     * Find holdings with quantity greater than zero
     * (Active holdings only)
     */
    @Query("SELECT h FROM PortfolioHolding h " +
            "WHERE h.portfolio.id = :portfolioId " +
            "AND h.quantity > 0")
    List<PortfolioHolding> findActiveHoldingsByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Count holdings by portfolio ID
     */
    long countByPortfolioId(Long portfolioId);

    /**
     * Count active holdings (quantity > 0) by portfolio ID
     */
    @Query("SELECT COUNT(h) FROM PortfolioHolding h " +
            "WHERE h.portfolio.id = :portfolioId " +
            "AND h.quantity > 0")
    long countActiveHoldingsByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Find holdings by instrument ID across all portfolios
     * (Useful for checking which users hold a specific instrument)
     */
    List<PortfolioHolding> findByInstrumentId(Long instrumentId);

    /**
     * Custom query: Find holdings by user ID (across all portfolios)
     */
    @Query("SELECT h FROM PortfolioHolding h " +
            "WHERE h.portfolio.userId = :userId")
    List<PortfolioHolding> findByUserId(@Param("userId") String userId);

    /**
     * Custom query: Sum total quantity for a specific instrument across all portfolios of a user
     */
    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM PortfolioHolding h " +
            "WHERE h.portfolio.userId = :userId " +
            "AND h.instrument.id = :instrumentId")
    BigDecimal sumQuantityByUserIdAndInstrumentId(
            @Param("userId") String userId,
            @Param("instrumentId") Long instrumentId
    );

    /**
     * Custom query: Find top holdings by value (requires current price calculation in service)
     * This returns holdings ordered by quantity × averageBuyPrice
     */
    @Query("SELECT h FROM PortfolioHolding h " +
            "WHERE h.portfolio.id = :portfolioId " +
            "ORDER BY (h.quantity * h.averageBuyPrice) DESC")
    List<PortfolioHolding> findTopHoldingsByValue(@Param("portfolioId") Long portfolioId);

    /**
     * Custom query: Find holdings purchased within a date range
     */
    @Query("SELECT h FROM PortfolioHolding h " +
            "WHERE h.portfolio.id = :portfolioId " +
            "AND h.firstPurchaseDate BETWEEN :startDate AND :endDate")
    List<PortfolioHolding> findByPurchaseDateBetween(
            @Param("portfolioId") Long portfolioId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Delete holding by portfolio ID and instrument ID
     */
    void deleteByPortfolioIdAndInstrumentId(Long portfolioId, Long instrumentId);

    /**
     * Delete all holdings for a portfolio
     */
    void deleteByPortfolioId(Long portfolioId);

    void deleteAllByPortfolioId(Long portfolioId);
}