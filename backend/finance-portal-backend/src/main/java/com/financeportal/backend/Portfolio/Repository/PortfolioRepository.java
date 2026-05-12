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

    List<Portfolio> findByUserId(String userId);

    Page<Portfolio> findByUserId(String userId, Pageable pageable);

    List<Portfolio> findByUserIdAndActiveTrue(String userId);

    Optional<Portfolio> findByIdAndUserId(Long id, String userId);

    List<Portfolio> findByUserIdAndPortfolioType(String userId, PortfolioType portfolioType);

    boolean existsByIdAndUserId(Long id, String userId);

    long countByUserId(String userId);

    long countByUserIdAndActiveTrue(String userId);

    List<Portfolio> findByUserIdAndCreatedAtAfter(String userId, LocalDateTime date);

    @Query("SELECT p FROM Portfolio p " +
            "LEFT JOIN FETCH p.holdings h " +
            "LEFT JOIN FETCH h.instrument " +
            "WHERE p.userId = :userId")
    List<Portfolio> findByUserIdWithHoldings(@Param("userId") String userId);

    @Query("SELECT DISTINCT p FROM Portfolio p " +
            "LEFT JOIN FETCH p.holdings h " +
            "LEFT JOIN FETCH h.instrument " +
            "LEFT JOIN FETCH p.transactions " +
            "WHERE p.id = :portfolioId AND p.userId = :userId")
    Optional<Portfolio> findByIdAndUserIdWithDetails(
            @Param("portfolioId") Long portfolioId,
            @Param("userId") String userId
    );

    @Query("SELECT DISTINCT p FROM Portfolio p " +
            "INNER JOIN p.holdings " +
            "WHERE p.userId = :userId")
    List<Portfolio> findByUserIdWithActiveHoldings(@Param("userId") String userId);

    @Query("SELECT p FROM Portfolio p " +
            "WHERE p.userId = :userId " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Portfolio> searchByName(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm
    );

    @Query("SELECT p FROM Portfolio p WHERE p.userId LIKE CONCAT('%', :userId, '%')")
    List<Portfolio> findByUserIdContaining(@Param("userId") String userId);

    void deleteByIdAndUserId(Long id, String userId);
}
