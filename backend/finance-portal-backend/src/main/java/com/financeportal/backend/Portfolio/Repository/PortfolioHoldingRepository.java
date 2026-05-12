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

    List<PortfolioHolding> findByPortfolioId(Long portfolioId);

    @Query("SELECT h FROM PortfolioHolding h " +
            "LEFT JOIN FETCH h.instrument " +
            "WHERE h.portfolio.id = :portfolioId")
    List<PortfolioHolding> findByPortfolioIdWithInstrument(@Param("portfolioId") Long portfolioId);

    Optional<PortfolioHolding> findByPortfolioIdAndInstrumentId(Long portfolioId, Long instrumentId);

    Optional<PortfolioHolding> findByPortfolioAndInstrument(Portfolio portfolio, BaseInstrument instrument);

    boolean existsByPortfolioIdAndInstrumentId(Long portfolioId, Long instrumentId);

    @Query("SELECT h FROM PortfolioHolding h " +
            "WHERE h.portfolio.id = :portfolioId " +
            "AND h.quantity > 0")
    List<PortfolioHolding> findActiveHoldingsByPortfolioId(@Param("portfolioId") Long portfolioId);

    long countByPortfolioId(Long portfolioId);

    @Query("SELECT COUNT(h) FROM PortfolioHolding h " +
            "WHERE h.portfolio.id = :portfolioId " +
            "AND h.quantity > 0")
    long countActiveHoldingsByPortfolioId(@Param("portfolioId") Long portfolioId);

    List<PortfolioHolding> findByInstrumentId(Long instrumentId);

    @Query("SELECT h FROM PortfolioHolding h " +
            "WHERE h.portfolio.userId = :userId")
    List<PortfolioHolding> findByUserId(@Param("userId") String userId);

    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM PortfolioHolding h " +
            "WHERE h.portfolio.userId = :userId " +
            "AND h.instrument.id = :instrumentId")
    BigDecimal sumQuantityByUserIdAndInstrumentId(
            @Param("userId") String userId,
            @Param("instrumentId") Long instrumentId
    );

    @Query("SELECT h FROM PortfolioHolding h " +
            "WHERE h.portfolio.id = :portfolioId " +
            "ORDER BY (h.quantity * h.averageBuyPrice) DESC")
    List<PortfolioHolding> findTopHoldingsByValue(@Param("portfolioId") Long portfolioId);

    @Query("SELECT h FROM PortfolioHolding h " +
            "WHERE h.portfolio.id = :portfolioId " +
            "AND h.firstPurchaseDate BETWEEN :startDate AND :endDate")
    List<PortfolioHolding> findByPurchaseDateBetween(
            @Param("portfolioId") Long portfolioId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    void deleteByPortfolioIdAndInstrumentId(Long portfolioId, Long instrumentId);

    void deleteByPortfolioId(Long portfolioId);

    void deleteAllByPortfolioId(Long portfolioId);
}