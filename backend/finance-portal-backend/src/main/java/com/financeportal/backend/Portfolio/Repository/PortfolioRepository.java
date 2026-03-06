package com.financeportal.backend.Portfolio.Repository;

import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * Find all portfolios by user ID
     */
    List<Portfolio> findByUserId(String userId);

    /**
     * Find all portfolios by user ID (paginated)
     */
    Page<Portfolio> findByUserId(String userId, Pageable pageable);

    /**
     * Find active portfolios by user ID
     */
    List<Portfolio> findByUserIdAndActiveTrue(String userId);

    /**
     * Find portfolio by ID and user ID (ownership check)
     */
    Optional<Portfolio> findByIdAndUserId(Long id, String userId);

    /**
     * Find portfolios by type
     */
    List<Portfolio> findByUserIdAndPortfolioType(String userId, PortfolioType portfolioType);

    /**
     * Check if portfolio exists for user
     */
    boolean existsByIdAndUserId(Long id, String userId);

    /**
     * Count portfolios by user ID
     */
    long countByUserId(String userId);

    /**
     * Count active portfolios by user ID
     */
    long countByUserIdAndActiveTrue(String userId);

    /**
     * Find portfolios created after a specific date
     */
    List<Portfolio> findByUserIdAndCreatedAtAfter(String userId, LocalDateTime date);

    /**
     * Custom query: Find portfolios with holdings count
     * (Useful for dashboard/summary)
     */
    @Query("SELECT p FROM Portfolio p " +
            "LEFT JOIN FETCH p.holdings " +
            "WHERE p.userId = :userId")
    List<Portfolio> findByUserIdWithHoldings(@Param("userId") String userId);

    /**
     * Custom query: Find portfolio with holdings and transactions
     * (For detail page - fetch all related data in one query)
     */
    @Query("SELECT DISTINCT p FROM Portfolio p " +
            "LEFT JOIN FETCH p.holdings h " +
            "LEFT JOIN FETCH h.instrument " +
            "LEFT JOIN FETCH p.transactions " +
            "WHERE p.id = :portfolioId AND p.userId = :userId")
    Optional<Portfolio> findByIdAndUserIdWithDetails(
            @Param("portfolioId") Long portfolioId,
            @Param("userId") String userId
    );

    /**
     * Custom query: Find portfolios with at least one holding
     */
    @Query("SELECT DISTINCT p FROM Portfolio p " +
            "INNER JOIN p.holdings " +
            "WHERE p.userId = :userId")
    List<Portfolio> findByUserIdWithActiveHoldings(@Param("userId") String userId);

    /**
     * Custom query: Search portfolios by name
     */
    @Query("SELECT p FROM Portfolio p " +
            "WHERE p.userId = :userId " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Portfolio> searchByName(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm
    );

    /**
     * Delete portfolio by ID and user ID (ownership check)
     */
    void deleteByIdAndUserId(Long id, String userId);
}
