package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Exception.UnauthorizedException;
import com.financeportal.backend.Portfolio.DTO.*;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapper;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingService holdingService;
    private final PortfolioMapper portfolioMapper;

    // Mock user ID (will be replaced with SecurityContextHolder.getContext().getAuthentication())
    private static final String MOCK_USER_ID = "mock-user-001";

    @Override
    @Transactional
    public PortfolioDTO createPortfolio(CreatePortfolioRequestDTO request) {
        log.info("Creating new portfolio: {} for user: {}", request.getName(), MOCK_USER_ID);

        // Map request to entity
        Portfolio portfolio = portfolioMapper.toEntity(request);
        portfolio.setUserId(MOCK_USER_ID);

        // Save
        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Portfolio created successfully with ID: {}", saved.getId());

        // Map to DTO
        PortfolioDTO dto = portfolioMapper.toDTO(saved);

        // Set initial calculated fields (empty portfolio)
        dto.setTotalValue(saved.getInitialBalance());
        dto.setTotalInvested(BigDecimal.ZERO);
        dto.setUnrealizedPnL(BigDecimal.ZERO);
        dto.setPnlPercent(BigDecimal.ZERO);
        dto.setHoldingCount(0);

        return dto;
    }

    @Override
    @Transactional
    public PortfolioDTO updatePortfolio(Long portfolioId, UpdatePortfolioRequestDTO request) {
        log.info("Updating portfolio ID: {}", portfolioId);

        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);

        // Update fields if provided
        if (request.getName() != null) {
            portfolio.setName(request.getName());
        }
        if (request.getDescription() != null) {
            portfolio.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            portfolio.setActive(request.getActive());
        }

        Portfolio updated = portfolioRepository.save(portfolio);
        log.info("Portfolio updated successfully: {}", portfolioId);

        return enrichPortfolioDTO(portfolioMapper.toDTO(updated), updated);
    }

    @Override
    @Transactional
    public void deletePortfolio(Long portfolioId) {
        log.info("Soft deleting portfolio ID: {}", portfolioId);

        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        portfolio.setActive(false);
        portfolioRepository.save(portfolio);

        log.info("Portfolio soft deleted: {}", portfolioId);
    }

    @Override
    @Transactional
    public void hardDeletePortfolio(Long portfolioId) {
        log.warn("Hard deleting portfolio ID: {}", portfolioId);

        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        portfolioRepository.delete(portfolio);

        log.warn("Portfolio permanently deleted: {}", portfolioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getUserPortfolios() {
        log.info("Fetching all portfolios for user: {}", MOCK_USER_ID);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(MOCK_USER_ID);

        return portfolios.stream()
                .map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PortfolioDTO> getUserPortfolios(Pageable pageable) {
        log.info("Fetching portfolios for user: {} (paginated)", MOCK_USER_ID);

        Page<Portfolio> portfolioPage = portfolioRepository.findByUserId(MOCK_USER_ID, pageable);

        return portfolioPage.map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getActivePortfolios() {
        log.info("Fetching active portfolios for user: {}", MOCK_USER_ID);

        List<Portfolio> portfolios = portfolioRepository.findByUserIdAndActiveTrue(MOCK_USER_ID);

        return portfolios.stream()
                .map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioDetailDTO getPortfolioDetail(Long portfolioId) {
        log.info("Fetching portfolio detail for ID: {}", portfolioId);

        try {
            // ⭐ BASİT findById kullan (complex method yerine)
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

            log.info("Portfolio found: {}", portfolio.getName());

            // Ownership check
            if (!portfolio.getUserId().equals(MOCK_USER_ID)) {
                throw new UnauthorizedException("You don't have permission to access this portfolio");
            }

            log.info("Ownership verified");

            // Map to detail DTO
            PortfolioDetailDTO dto = portfolioMapper.toDetailDTO(portfolio);

            log.info("Mapped to DTO");

            // Get holdings with current prices
            List<HoldingDTO> holdings = holdingService.getHoldingsByPortfolioId(portfolioId);

            log.info("Holdings fetched: {}", holdings.size());

            dto.setHoldings(holdings);

            // Calculate summary metrics
            BigDecimal totalInvested = holdings.stream()
                    .map(HoldingDTO::getTotalInvestment)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal currentValue = holdings.stream()
                    .map(HoldingDTO::getCurrentValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal unrealizedPnL = currentValue.subtract(totalInvested);

            BigDecimal pnlPercent = BigDecimal.ZERO;
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                pnlPercent = unrealizedPnL
                        .divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal cashBalance = portfolio.getInitialBalance().subtract(totalInvested);

            // Set calculated fields
            dto.setTotalInvested(totalInvested);
            dto.setCurrentValue(currentValue);
            dto.setUnrealizedPnL(unrealizedPnL);
            dto.setPnlPercent(pnlPercent);
            dto.setCashBalance(cashBalance);
            dto.setTotalHoldings(holdings.size());
            dto.setTotalTransactions(0);  // ⭐ Şimdilik 0 (transactions lazy loading problemi yaratıyor)

            log.info("Portfolio detail fetched successfully. Holdings: {}, Total Value: {}",
                    holdings.size(), currentValue);

            return dto;

        } catch (Exception e) {
            log.error("ERROR in getPortfolioDetail: ", e);  // ⭐ FULL STACK TRACE
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolioById(Long portfolioId) {
        log.info("Fetching portfolio by ID: {}", portfolioId);

        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);

        return enrichPortfolioDTO(portfolioMapper.toDTO(portfolio), portfolio);
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioSummaryDTO getPortfolioSummary() {
        log.info("Fetching portfolio summary for user: {}", MOCK_USER_ID);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(MOCK_USER_ID);

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        List<PortfolioDTO> allPortfolios = new ArrayList<>();

        for (Portfolio portfolio : portfolios) {
            PortfolioDTO dto = enrichPortfolioDTO(portfolioMapper.toDTO(portfolio), portfolio);
            allPortfolios.add(dto);

            totalValue = totalValue.add(dto.getTotalValue());
            totalInvested = totalInvested.add(dto.getTotalInvested());
        }

        BigDecimal totalUnrealizedPnL = totalValue.subtract(totalInvested);
        BigDecimal totalPnLPercent = BigDecimal.ZERO;

        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalPnLPercent = totalUnrealizedPnL
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Sort by P&L percent
        List<PortfolioDTO> sortedByPnL = allPortfolios.stream()
                .sorted((p1, p2) -> p2.getPnlPercent().compareTo(p1.getPnlPercent()))
                .collect(Collectors.toList());

        List<PortfolioDTO> topPerformers = sortedByPnL.stream().limit(3).collect(Collectors.toList());
        List<PortfolioDTO> worstPerformers = sortedByPnL.stream()
                .skip(Math.max(0, sortedByPnL.size() - 3))
                .collect(Collectors.toList());

        // Asset allocation (aggregate across all portfolios)
        List<AssetAllocationDTO> assetAllocation = calculateAggregateAssetAllocation(portfolios);

        return PortfolioSummaryDTO.builder()
                .totalPortfolios(portfolios.size())
                .totalValue(totalValue)
                .totalInvested(totalInvested)
                .totalUnrealizedPnL(totalUnrealizedPnL)
                .totalPnLPercent(totalPnLPercent)
                .topPerformers(topPerformers)
                .worstPerformers(worstPerformers)
                .assetAllocation(assetAllocation)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioPerformanceDTO getPortfolioPerformance(Long portfolioId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating portfolio performance for ID: {} from {} to {}", portfolioId, startDate, endDate);

        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);

        BigDecimal currentValue = holdingService.calculateCurrentValue(portfolioId);
        BigDecimal totalInvested = holdingService.calculateTotalInvestment(portfolioId);

        BigDecimal totalReturn = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalReturn = currentValue.subtract(totalInvested)
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return PortfolioPerformanceDTO.builder()
                .portfolioId(portfolioId)
                .portfolioName(portfolio.getName())
                .dailyReturn(BigDecimal.ZERO)
                .weeklyReturn(BigDecimal.ZERO)
                .monthlyReturn(BigDecimal.ZERO)
                .yearlyReturn(BigDecimal.ZERO)
                .totalReturn(totalReturn)
                .historicalData(new ArrayList<>())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPortfolioValue() {
        log.info("Calculating total portfolio value for user: {}", MOCK_USER_ID);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(MOCK_USER_ID);

        return portfolios.stream()
                .map(p -> holdingService.calculateCurrentValue(p.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalUnrealizedPnL() {
        log.info("Calculating total unrealized P&L for user: {}", MOCK_USER_ID);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(MOCK_USER_ID);

        return portfolios.stream()
                .map(p -> holdingService.calculateUnrealizedPnL(p.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional
    public void activatePortfolio(Long portfolioId) {
        log.info("Activating portfolio ID: {}", portfolioId);

        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        portfolio.setActive(true);
        portfolioRepository.save(portfolio);

        log.info("Portfolio activated: {}", portfolioId);
    }

    @Override
    @Transactional
    public void deactivatePortfolio(Long portfolioId) {
        log.info("Deactivating portfolio ID: {}", portfolioId);

        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        portfolio.setActive(false);
        portfolioRepository.save(portfolio);

        log.info("Portfolio deactivated: {}", portfolioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> searchPortfoliosByName(String searchTerm) {
        log.info("Searching portfolios by name: {}", searchTerm);

        List<Portfolio> portfolios = portfolioRepository.searchByName(MOCK_USER_ID, searchTerm);

        return portfolios.stream()
                .map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getPortfoliosByType(String portfolioType) {
        log.info("Fetching portfolios by type: {}", portfolioType);

        PortfolioType type = PortfolioType.valueOf(portfolioType.toUpperCase());
        List<Portfolio> portfolios = portfolioRepository.findByUserIdAndPortfolioType(MOCK_USER_ID, type);

        return portfolios.stream()
                .map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p))
                .collect(Collectors.toList());
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Get portfolio entity and check ownership
     */
    private Portfolio getPortfolioEntityWithOwnershipCheck(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        if (!portfolio.getUserId().equals(MOCK_USER_ID)) {
            log.error("Unauthorized access attempt to portfolio: {} by user: {}", portfolioId, MOCK_USER_ID);
            throw new UnauthorizedException("You don't have permission to access this portfolio");
        }

        return portfolio;
    }

    /**
     * Enrich PortfolioDTO with calculated fields
     */
    private PortfolioDTO enrichPortfolioDTO(PortfolioDTO dto, Portfolio portfolio) {
        BigDecimal totalInvested = holdingService.calculateTotalInvestment(portfolio.getId());
        BigDecimal currentValue = holdingService.calculateCurrentValue(portfolio.getId());
        BigDecimal unrealizedPnL = currentValue.subtract(totalInvested);

        BigDecimal pnlPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            pnlPercent = unrealizedPnL
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        dto.setTotalValue(currentValue);
        dto.setTotalInvested(totalInvested);
        dto.setUnrealizedPnL(unrealizedPnL);
        dto.setPnlPercent(pnlPercent);
        dto.setHoldingCount(portfolio.getHoldings().size());

        return dto;
    }

    /**
     * Calculate aggregate asset allocation across all portfolios
     */
    private List<AssetAllocationDTO> calculateAggregateAssetAllocation(List<Portfolio> portfolios) {

        return new ArrayList<>(); // Placeholder
    }
}
