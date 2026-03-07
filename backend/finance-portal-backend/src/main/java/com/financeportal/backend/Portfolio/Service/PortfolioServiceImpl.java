package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Exception.UnauthorizedException;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Portfolio.DTO.*;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Entity.PortfolioTransaction;
import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingService holdingService;
    private final PortfolioMapper portfolioMapper;
    private final InstrumentPriceRepository instrumentPriceRepository;
    private final InstrumentRepository instrumentRepository;

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

            BigDecimal holdingsValue = holdings.stream()
                    .map(HoldingDTO::getCurrentValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // ⭐ Total Value = Cash + Holdings
            BigDecimal cashBalance = portfolio.getInitialBalance().subtract(totalInvested);
            BigDecimal totalValue = cashBalance.add(holdingsValue);

            BigDecimal unrealizedPnL = holdingsValue.subtract(totalInvested);

            BigDecimal pnlPercent = BigDecimal.ZERO;
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                pnlPercent = unrealizedPnL
                        .divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // Set calculated fields
            dto.setTotalInvested(totalInvested);
            dto.setCurrentValue(totalValue);  // ⭐ Nakit + Varlıklar
            dto.setUnrealizedPnL(unrealizedPnL);
            dto.setPnlPercent(pnlPercent);
            dto.setCashBalance(cashBalance);
            dto.setTotalHoldings(holdings.size());
            dto.setTotalTransactions(0);

            log.info("Portfolio detail fetched successfully. Holdings: {}, Total Value: {}",
                    holdings.size(), totalValue);

            return dto;

        } catch (Exception e) {
            log.error("ERROR in getPortfolioDetail: ", e);
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

        // ⭐ Generate historical data points
        List<PerformanceDataPointDTO> historicalData = generateHistoricalPerformanceData(
                portfolio, startDate, endDate, totalInvested, currentValue
        );

        return PortfolioPerformanceDTO.builder()
                .portfolioId(portfolioId)
                .portfolioName(portfolio.getName())
                .dailyReturn(BigDecimal.ZERO)
                .weeklyReturn(BigDecimal.ZERO)
                .monthlyReturn(BigDecimal.ZERO)
                .yearlyReturn(BigDecimal.ZERO)
                .totalReturn(totalReturn)
                .historicalData(historicalData)
                .build();
    }

    /**
     * Generate historical performance data points based on actual transaction history
     *
     * This calculates portfolio value for each day by:
     * 1. Starting with initial balance
     * 2. Processing transactions chronologically
     * 3. Marking current holdings to market each day
     */
    private List<PerformanceDataPointDTO> generateHistoricalPerformanceData(
            Portfolio portfolio, LocalDate startDate, LocalDate endDate,
            BigDecimal totalInvested, BigDecimal currentValue) {

        log.info("Generating historical performance data from {} to {}", startDate, endDate);

        List<PerformanceDataPointDTO> dataPoints = new ArrayList<>();

        // Get all transactions for this portfolio
        List<PortfolioTransaction> transactions = portfolio.getTransactions().stream()
                .sorted(Comparator.comparing(PortfolioTransaction::getTransactionDate))
                .collect(Collectors.toList());

        log.info("Found {} transactions for portfolio {}", transactions.size(), portfolio.getId());

        // If no transactions, return flat line at initial balance
        if (transactions.isEmpty()) {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                BigDecimal initialBalance = portfolio.getInitialBalance();

                PerformanceDataPointDTO dataPoint = PerformanceDataPointDTO.builder()
                        .date(currentDate)
                        .value(initialBalance.setScale(2, RoundingMode.HALF_UP))
                        .returnPercent(BigDecimal.ZERO)
                        .build();

                dataPoints.add(dataPoint);
                currentDate = currentDate.plusDays(1);
            }

            log.info("No transactions - generated {} data points at initial balance", dataPoints.size());
            return dataPoints;
        }

        // Track holdings over time (instrumentId -> quantity)
        Map<Long, BigDecimal> holdingsMap = new HashMap<>();

        // Track cash balance
        BigDecimal cashBalance = portfolio.getInitialBalance();

        // Process each day
        LocalDate currentDate = startDate;
        int transactionIndex = 0;

        while (!currentDate.isAfter(endDate)) {
            // Process all transactions for this day
            while (transactionIndex < transactions.size()) {
                PortfolioTransaction tx = transactions.get(transactionIndex);
                LocalDate txDate = tx.getTransactionDate().toLocalDate();

                if (txDate.isAfter(currentDate)) {
                    break; // Future transaction, stop
                }

                if (txDate.isBefore(startDate)) {
                    // Past transaction before our window - still need to process for holdings
                    processTransaction(tx, holdingsMap, cashBalance);
                    transactionIndex++;
                    continue;
                }

                // Transaction is on or before current date
                if (!txDate.isAfter(currentDate)) {
                    BigDecimal txCashImpact = processTransaction(tx, holdingsMap, cashBalance);
                    cashBalance = cashBalance.add(txCashImpact);
                    transactionIndex++;
                } else {
                    break;
                }
            }

            // Calculate total portfolio value for this date
            BigDecimal holdingsValue = calculateHoldingsValueAtDate(holdingsMap, currentDate);
            BigDecimal totalValueAtDate = cashBalance.add(holdingsValue);

            // Calculate return percentage
            BigDecimal returnPercent = BigDecimal.ZERO;
            BigDecimal initialBalance = portfolio.getInitialBalance();
            if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
                returnPercent = totalValueAtDate.subtract(initialBalance)
                        .divide(initialBalance, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            PerformanceDataPointDTO dataPoint = PerformanceDataPointDTO.builder()
                    .date(currentDate)
                    .value(totalValueAtDate.setScale(2, RoundingMode.HALF_UP))
                    .returnPercent(returnPercent)
                    .build();

            dataPoints.add(dataPoint);
            currentDate = currentDate.plusDays(1);
        }

        log.info("Generated {} historical data points based on transaction history", dataPoints.size());

        return dataPoints;
    }

    /**
     * Process a transaction and update holdings map
     * Returns cash impact (negative for buy, positive for sell)
     */
    private BigDecimal processTransaction(PortfolioTransaction tx,
                                          Map<Long, BigDecimal> holdingsMap,
                                          BigDecimal currentCashBalance) {
        Long instrumentId = tx.getInstrument().getId();
        BigDecimal quantity = tx.getQuantity();

        if (tx.getTransactionType() == TransactionType.BUY) {
            // Add to holdings
            holdingsMap.merge(instrumentId, quantity, BigDecimal::add);

            // Cash out (negative)
            BigDecimal cashImpact = tx.getTotalAmount().negate();
            if (tx.getCommission() != null) {
                cashImpact = cashImpact.subtract(tx.getCommission());
            }
            if (tx.getTax() != null) {
                cashImpact = cashImpact.subtract(tx.getTax());
            }
            return cashImpact;

        } else { // SELL
            // Remove from holdings
            holdingsMap.merge(instrumentId, quantity.negate(), BigDecimal::add);
            if (holdingsMap.get(instrumentId).compareTo(BigDecimal.ZERO) == 0) {
                holdingsMap.remove(instrumentId);
            }

            // Cash in (positive)
            BigDecimal cashImpact = tx.getTotalAmount();
            if (tx.getCommission() != null) {
                cashImpact = cashImpact.subtract(tx.getCommission());
            }
            if (tx.getTax() != null) {
                cashImpact = cashImpact.subtract(tx.getTax());
            }
            return cashImpact;
        }
    }

    /**
     * Calculate total value of holdings at a specific date using current prices
     *
     * NOTE: This uses CURRENT prices for simplicity.
     * For true historical accuracy, you would need historical price data.
     */
    private BigDecimal calculateHoldingsValueAtDate(Map<Long, BigDecimal> holdingsMap,
                                                    LocalDate date) {
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Map.Entry<Long, BigDecimal> entry : holdingsMap.entrySet()) {
            Long instrumentId = entry.getKey();
            BigDecimal quantity = entry.getValue();

            // Get current price (in production, get historical price for 'date')
            BigDecimal currentPrice = getCurrentPriceForInstrument(instrumentId);

            BigDecimal holdingValue = quantity.multiply(currentPrice);
            totalValue = totalValue.add(holdingValue);
        }

        return totalValue;
    }

    /**
     * Get current price for an instrument
     */
    private BigDecimal getCurrentPriceForInstrument(Long instrumentId) {
        try {
            // First get the instrument entity
            BaseInstrument instrument = instrumentRepository.findById(instrumentId)
                    .orElse(null);

            if (instrument == null) {
                log.warn("Instrument not found: {}", instrumentId);
                return BigDecimal.ZERO;
            }

            // Then get its latest price
            return instrumentPriceRepository
                    .findTopByInstrumentOrderByTimestampDesc(instrument)
                    .map(InstrumentPrice::getCurrentPrice)
                    .orElse(BigDecimal.ZERO);

        } catch (Exception e) {
            log.error("Error getting price for instrument {}: {}", instrumentId, e.getMessage());
            return BigDecimal.ZERO;
        }
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
        BigDecimal holdingsValue = holdingService.calculateCurrentValue(portfolio.getId());

        // Total Value = Cash + Holdings
        BigDecimal cashBalance = portfolio.getInitialBalance().subtract(totalInvested);
        BigDecimal totalValue = cashBalance.add(holdingsValue);

        BigDecimal unrealizedPnL = holdingsValue.subtract(totalInvested);

        BigDecimal pnlPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            pnlPercent = unrealizedPnL
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        dto.setTotalValue(totalValue);
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
