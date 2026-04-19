package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Portfolio.DTO.AssetAllocationDTO;
import com.financeportal.backend.Portfolio.DTO.HoldingDTO;
import com.financeportal.backend.Portfolio.Entity.PortfolioHolding;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapper;
import com.financeportal.backend.Portfolio.Repository.PortfolioHoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioHoldingServiceImpl implements PortfolioHoldingService {

    private final PortfolioHoldingRepository holdingRepository;
    private final InstrumentPriceRepository priceRepository;
    private final PortfolioCalculationService calculationService;
    private final PortfolioMapper portfolioMapper;

    @Override
    @Transactional(readOnly = true)
    public List<HoldingDTO> getHoldingsByPortfolioId(Long portfolioId) {
        log.info("Fetching holdings for portfolio ID: {}", portfolioId);

        List<PortfolioHolding> holdings = holdingRepository.findByPortfolioIdWithInstrument(portfolioId);

        return holdings.stream()
                .map(this::enrichHoldingDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HoldingDTO getHoldingById(Long holdingId) {
        log.info("Fetching holding by ID: {}", holdingId);

        PortfolioHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding not found with id: " + holdingId));

        return enrichHoldingDTO(holding);
    }

    @Override
    @Transactional(readOnly = true)
    public HoldingDTO getHoldingByPortfolioAndInstrument(Long portfolioId, Long instrumentId) {
        log.info("Fetching holding for portfolio: {} and instrument: {}", portfolioId, instrumentId);

        Optional<PortfolioHolding> holdingOpt = holdingRepository
                .findByPortfolioIdAndInstrumentId(portfolioId, instrumentId);

        return holdingOpt.map(this::enrichHoldingDTO).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoldingDTO> getActiveHoldings(Long portfolioId) {
        log.info("Fetching active holdings for portfolio ID: {}", portfolioId);

        List<PortfolioHolding> holdings = holdingRepository.findActiveHoldingsByPortfolioId(portfolioId);

        return holdings.stream()
                .map(this::enrichHoldingDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoldingDTO> getTopHoldingsByValue(Long portfolioId, int limit) {
        log.info("Fetching top {} holdings by value for portfolio ID: {}", limit, portfolioId);

        List<HoldingDTO> allHoldings = getHoldingsByPortfolioId(portfolioId);

        return allHoldings.stream()
                .sorted(Comparator.comparing(HoldingDTO::getCurrentValue).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetAllocationDTO> getAssetAllocation(Long portfolioId) {
        log.info("Calculating asset allocation for portfolio ID: {}", portfolioId);

        List<HoldingDTO> holdings = getHoldingsByPortfolioId(portfolioId);

        // Group by instrument type
        Map<String, List<HoldingDTO>> groupedByType = holdings.stream()
                .collect(Collectors.groupingBy(HoldingDTO::getInstrumentType));

        BigDecimal totalValue = holdings.stream()
                .map(HoldingDTO::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AssetAllocationDTO> allocation = new ArrayList<>();

        for (Map.Entry<String, List<HoldingDTO>> entry : groupedByType.entrySet()) {
            String type = entry.getKey();
            List<HoldingDTO> typeHoldings = entry.getValue();

            BigDecimal typeValue = typeHoldings.stream()
                    .map(HoldingDTO::getCurrentValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal percentage = BigDecimal.ZERO;
            if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
                percentage = typeValue
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            allocation.add(AssetAllocationDTO.builder()
                    .instrumentType(type)
                    .totalValue(typeValue)
                    .percentage(percentage)
                    .count(typeHoldings.size())
                    .build());
        }

        // Sort by value descending
        allocation.sort(Comparator.comparing(AssetAllocationDTO::getTotalValue).reversed());

        return allocation;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalInvestment(Long portfolioId) {
        log.debug("Calculating total investment for portfolio ID: {}", portfolioId);

        List<PortfolioHolding> holdings = holdingRepository.findByPortfolioId(portfolioId);

        return holdings.stream()
                .map(h -> calculationService.calculateTotalInvestment(h.getQuantity(), h.getAverageBuyPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateCurrentValue(Long portfolioId) {
        log.debug("Calculating current value for portfolio ID: {}", portfolioId);

        List<PortfolioHolding> holdings = holdingRepository.findByPortfolioIdWithInstrument(portfolioId);

        return holdings.stream()
                .map(h -> {
                    BigDecimal currentPrice = getCurrentPrice(h.getInstrument());
                    return calculationService.calculateCurrentValue(h.getQuantity(), currentPrice);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateUnrealizedPnL(Long portfolioId) {
        log.debug("Calculating unrealized P&L for portfolio ID: {}", portfolioId);

        BigDecimal currentValue = calculateCurrentValue(portfolioId);
        BigDecimal totalInvestment = calculateTotalInvestment(portfolioId);

        return currentValue.subtract(totalInvestment);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateHoldingUnrealizedPnL(Long holdingId) {
        log.debug("Calculating unrealized P&L for holding ID: {}", holdingId);

        PortfolioHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding not found with id: " + holdingId));

        BigDecimal currentPrice = getCurrentPrice(holding.getInstrument());

        return calculationService.calculateUnrealizedPnL(
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                currentPrice
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean holdingExists(Long portfolioId, Long instrumentId) {
        return holdingRepository.existsByPortfolioIdAndInstrumentId(portfolioId, instrumentId);
    }

    @Override
    @Transactional
    public void deleteHolding(Long holdingId) {
        log.info("Deleting holding ID: {}", holdingId);

        if (!holdingRepository.existsById(holdingId)) {
            throw new ResourceNotFoundException("Holding not found with id: " + holdingId);
        }

        holdingRepository.deleteById(holdingId);
        log.info("Holding deleted successfully: {}", holdingId);
    }

    @Override
    @Transactional
    public int deleteZeroQuantityHoldings(Long portfolioId) {
        log.info("Deleting zero quantity holdings for portfolio ID: {}", portfolioId);

        List<PortfolioHolding> holdings = holdingRepository.findByPortfolioId(portfolioId);

        int deletedCount = 0;
        for (PortfolioHolding holding : holdings) {
            if (holding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                holdingRepository.delete(holding);
                deletedCount++;
            }
        }

        log.info("Deleted {} zero quantity holdings", deletedCount);
        return deletedCount;
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Enrich HoldingDTO with current price and calculated fields
     */
    private HoldingDTO enrichHoldingDTO(PortfolioHolding holding) {
        BigDecimal currentPrice = getCurrentPrice(holding.getInstrument());

        HoldingDTO dto = portfolioMapper.toHoldingDTO(holding, currentPrice);

        // Calculate fields
        BigDecimal totalInvestment = calculationService.calculateTotalInvestment(
                holding.getQuantity(),
                holding.getAverageBuyPrice()
        );

        BigDecimal currentValue = calculationService.calculateCurrentValue(
                holding.getQuantity(),
                currentPrice
        );

        BigDecimal unrealizedPnL = calculationService.calculateUnrealizedPnL(
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                currentPrice
        );

        BigDecimal pnlPercent = calculationService.calculatePnLPercent(
                holding.getAverageBuyPrice(),
                currentPrice
        );

        // Set calculated fields
        dto.setTotalInvestment(totalInvestment);
        dto.setCurrentValue(currentValue);
        dto.setUnrealizedPnL(unrealizedPnL);
        dto.setPnlPercent(pnlPercent);

        return dto;
    }

    /**
     * Get current price for an instrument
     */
    private BigDecimal getCurrentPrice(BaseInstrument instrument) {
        return priceRepository.findTopByInstrumentOrderByTimestampDesc(instrument)
                .map(InstrumentPrice::getCurrentPrice)
                .orElse(BigDecimal.ZERO);
    }
}
