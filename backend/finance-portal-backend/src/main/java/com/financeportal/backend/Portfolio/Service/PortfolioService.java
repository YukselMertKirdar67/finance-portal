package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Portfolio.DTO.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PortfolioService {

    /**
     * Create a new portfolio for the current user
     *
     * @param request Portfolio creation request
     * @return Created portfolio DTO
     */
    PortfolioDTO createPortfolio(CreatePortfolioRequestDTO request);

    /**
     * Update an existing portfolio
     *
     * @param portfolioId Portfolio ID
     * @param request Update request
     * @return Updated portfolio DTO
     */
    PortfolioDTO updatePortfolio(Long portfolioId, UpdatePortfolioRequestDTO request);

    /**
     * Delete a portfolio (soft delete - set active to false)
     *
     * @param portfolioId Portfolio ID
     */
    void deletePortfolio(Long portfolioId);

    /**
     * Hard delete a portfolio (permanent deletion)
     *
     * @param portfolioId Portfolio ID
     */
    void hardDeletePortfolio(Long portfolioId);

    /**
     * Get all portfolios for the current user
     *
     * @return List of portfolio DTOs with calculated fields
     */
    List<PortfolioDTO> getUserPortfolios();

    /**
     * Get all portfolios for the current user (paginated)
     *
     * @param pageable Pagination parameters
     * @return Page of portfolio DTOs
     */
    Page<PortfolioDTO> getUserPortfolios(Pageable pageable);

    /**
     * Get active portfolios only
     *
     * @return List of active portfolio DTOs
     */
    List<PortfolioDTO> getActivePortfolios();

    /**
     * Get portfolio detail with holdings and calculated metrics
     *
     * @param portfolioId Portfolio ID
     * @return Portfolio detail DTO
     */
    PortfolioDetailDTO getPortfolioDetail(Long portfolioId);

    /**
     * Get portfolio by ID (basic info)
     *
     * @param portfolioId Portfolio ID
     * @return Portfolio DTO
     */
    PortfolioDTO getPortfolioById(Long portfolioId);

    /**
     * Get portfolio summary for dashboard
     * Includes total value, P&L, asset allocation, etc.
     *
     * @return Portfolio summary DTO
     */
    PortfolioSummaryDTO getPortfolioSummary();

    /**
     * Get portfolio performance metrics
     *
     * @param portfolioId Portfolio ID
     * @param startDate Start date for performance calculation
     * @param endDate End date for performance calculation
     * @return Portfolio performance DTO
     */
    PortfolioPerformanceDTO getPortfolioPerformance(
            Long portfolioId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Calculate total portfolio value (all portfolios)
     *
     * @return Total value across all portfolios
     */
    BigDecimal calculateTotalPortfolioValue();

    /**
     * Calculate total unrealized P&L (all portfolios)
     *
     * @return Total unrealized P&L
     */
    BigDecimal calculateTotalUnrealizedPnL();

    /**
     * Activate a portfolio
     *
     * @param portfolioId Portfolio ID
     */
    void activatePortfolio(Long portfolioId);

    /**
     * Deactivate a portfolio
     *
     * @param portfolioId Portfolio ID
     */
    void deactivatePortfolio(Long portfolioId);

    /**
     * Search portfolios by name
     *
     * @param searchTerm Search term
     * @return List of matching portfolios
     */
    List<PortfolioDTO> searchPortfoliosByName(String searchTerm);

    /**
     * Get portfolios by type
     *
     * @param portfolioType Portfolio type (PERSONAL, BUSINESS, etc.)
     * @return List of portfolios of the specified type
     */
    List<PortfolioDTO> getPortfoliosByType(String portfolioType);
}
