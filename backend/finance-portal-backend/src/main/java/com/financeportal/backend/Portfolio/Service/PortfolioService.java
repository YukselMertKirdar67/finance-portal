package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Portfolio.DTO.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PortfolioService {

    PortfolioDTO createPortfolio(CreatePortfolioRequestDTO request);

    PortfolioDTO updatePortfolio(Long portfolioId, UpdatePortfolioRequestDTO request);

    void deletePortfolio(Long portfolioId);

    void hardDeletePortfolio(Long portfolioId);

    List<PortfolioDTO> getUserPortfolios();

    Page<PortfolioDTO> getUserPortfolios(Pageable pageable);

    List<PortfolioDTO> getActivePortfolios();

    PortfolioDetailDTO getPortfolioDetail(Long portfolioId);

    PortfolioDTO getPortfolioById(Long portfolioId);

    PortfolioSummaryDTO getPortfolioSummary();

    PortfolioPerformanceDTO getPortfolioPerformance(
            Long portfolioId,
            LocalDate startDate,
            LocalDate endDate
    );

    BigDecimal calculateTotalPortfolioValue();

    BigDecimal calculateTotalUnrealizedPnL();

    void activatePortfolio(Long portfolioId);

    void deactivatePortfolio(Long portfolioId);

    List<PortfolioDTO> searchPortfoliosByName(String searchTerm);

    List<PortfolioDTO> getPortfoliosByType(String portfolioType);
}
