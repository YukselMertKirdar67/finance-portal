package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Exception.UnauthorizedException;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Service.TcmbService;
import com.financeportal.backend.Portfolio.DTO.*;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Entity.PortfolioHolding;
import com.financeportal.backend.Portfolio.Entity.PortfolioTransaction;
import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapper;
import com.financeportal.backend.Portfolio.Repository.PortfolioHoldingRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final PortfolioHoldingRepository holdingRepository;
    private final TcmbService tcmbService;

    /**
     * Yeni portföy oluşturur ve başlangıç değerlerini sıfır olarak ayarlar.
     */

    @Override
    @Transactional
    public PortfolioDTO createPortfolio(CreatePortfolioRequestDTO request) {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        log.info("Creating new portfolio: {} for user: {}", request.getName(), currentUserId);

        Portfolio portfolio = portfolioMapper.toEntity(request);
        portfolio.setUserId(currentUserId);

        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Portfolio created successfully with ID: {}", saved.getId());

        PortfolioDTO dto = portfolioMapper.toDTO(saved);
        dto.setTotalValue(BigDecimal.ZERO);
        dto.setTotalInvested(BigDecimal.ZERO);
        dto.setUnrealizedPnL(BigDecimal.ZERO);
        dto.setPnlPercent(BigDecimal.ZERO);
        dto.setHoldingCount(0);

        return dto;
    }

    /**
     * Portföy adını, açıklamasını veya aktiflik durumunu günceller.
     */

    @Override
    @Transactional
    public PortfolioDTO updatePortfolio(Long portfolioId, UpdatePortfolioRequestDTO request) {
        log.info("Updating portfolio ID: {}", portfolioId);

        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);

        if (request.getName() != null) portfolio.setName(request.getName());
        if (request.getDescription() != null) portfolio.setDescription(request.getDescription());
        if (request.getActive() != null) portfolio.setActive(request.getActive());

        Portfolio updated = portfolioRepository.save(portfolio);
        log.info("Portfolio updated successfully: {}", portfolioId);

        return enrichPortfolioDTO(portfolioMapper.toDTO(updated), updated);
    }

    /**
     * Portföyü soft delete ile pasif hale getirir (active = false).
     */

    @Override
    @Transactional
    public void deletePortfolio(Long portfolioId) {
        log.info("Soft deleting portfolio ID: {}", portfolioId);
        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        portfolio.setActive(false);
        portfolioRepository.save(portfolio);
        log.info("Portfolio soft deleted: {}", portfolioId);
    }

    /**
     * Portföyü kalıcı olarak siler.
     */

    @Override
    @Transactional
    public void hardDeletePortfolio(Long portfolioId) {
        log.warn("Hard deleting portfolio ID: {}", portfolioId);
        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        portfolioRepository.delete(portfolio);
        log.warn("Portfolio permanently deleted: {}", portfolioId);
    }

    /**
     * Kullanıcının tüm portföylerini getirir.
     */

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getUserPortfolios() {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        log.info("Fetching all portfolios for user: {}", currentUserId);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(currentUserId);

        return portfolios.stream()
                .map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p))
                .collect(Collectors.toList());
    }

    /**
     * Kullanıcının portföylerini sayfalı olarak getirir.
     */

    @Override
    @Transactional(readOnly = true)
    public Page<PortfolioDTO> getUserPortfolios(Pageable pageable) {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        log.info("Fetching portfolios for user: {} (paginated)", currentUserId);

        Page<Portfolio> portfolioPage = portfolioRepository.findByUserId(currentUserId, pageable);

        return portfolioPage.map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p));
    }

    /**
     * Kullanıcının sadece aktif portföylerini getirir.
     */


    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getActivePortfolios() {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        log.info("Fetching active portfolios for user: {}", currentUserId);

        List<Portfolio> portfolios = portfolioRepository.findByUserIdAndActiveTrue(currentUserId);

        return portfolios.stream()
                .map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p))
                .collect(Collectors.toList());
    }

    /**
     * Portföyün detay bilgilerini getirir.
     * Holding listesi, toplam yatırım, güncel değer ve kâr/zarar içerir.
     */

    @Override
    @Transactional(readOnly = true)
    public PortfolioDetailDTO getPortfolioDetail(Long portfolioId) {
        log.info("Fetching portfolio detail for ID: {}", portfolioId);

        try {
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

            log.info("Portfolio found: {}", portfolio.getName());

            String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
            if (!portfolio.getUserId().equals(currentUserId)) {
                throw new UnauthorizedException("You don't have permission to access this portfolio");
            }

            log.info("Ownership verified");

            PortfolioDetailDTO dto = portfolioMapper.toDetailDTO(portfolio);
            log.info("Mapped to DTO");

            String portfolioCurrency = portfolio.getCurrency() != null ? portfolio.getCurrency() : "TRY";
            List<HoldingDTO> holdings = holdingService.getHoldingsByPortfolioId(portfolioId, portfolioCurrency);
            log.info("Holdings fetched: {}", holdings.size());

            dto.setHoldings(holdings);

            BigDecimal totalInvested = holdings.stream()
                    .map(HoldingDTO::getTotalInvestment)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal holdingsValue = holdings.stream()
                    .map(HoldingDTO::getCurrentValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal unrealizedPnL = holdingsValue.subtract(totalInvested);

            BigDecimal pnlPercent = BigDecimal.ZERO;
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                pnlPercent = unrealizedPnL
                        .divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            dto.setTotalInvested(totalInvested);
            dto.setCurrentValue(holdingsValue);
            dto.setUnrealizedPnL(unrealizedPnL);
            dto.setPnlPercent(pnlPercent);
            dto.setTotalHoldings(holdings.size());
            dto.setTotalTransactions(0);

            log.info("Portfolio detail fetched successfully. Holdings: {}, Total Value: {}",
                    holdings.size(), holdingsValue);

            return dto;

        } catch (Exception e) {
            log.error("ERROR in getPortfolioDetail: ", e);
            throw e;
        }
    }

    /**
     * ID'ye göre portföy getirir.
     */

    @Override
    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolioById(Long portfolioId) {
        log.info("Fetching portfolio by ID: {}", portfolioId);
        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        return enrichPortfolioDTO(portfolioMapper.toDTO(portfolio), portfolio);
    }

    /**
     * Kullanıcının tüm portföylerinin özetini getirir.
     * Tüm değerler TRY'ye çevrilerek toplanır.
     * En iyi ve en kötü performanslı portföyler listelenir.
     */

    @Override
    @Transactional(readOnly = true)
    public PortfolioSummaryDTO getPortfolioSummary() {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        log.info("Fetching portfolio summary for user: {}", currentUserId);

        List<Portfolio> portfolios = portfolioRepository.findByUserIdWithHoldings(currentUserId);

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalUnrealizedPnL = BigDecimal.ZERO;
        List<PortfolioDTO> allPortfolios = new ArrayList<>();

        for (Portfolio portfolio : portfolios) {
            PortfolioDTO dto = enrichPortfolioDTO(portfolioMapper.toDTO(portfolio), portfolio);
            allPortfolios.add(dto);

            String currency = portfolio.getCurrency() != null ? portfolio.getCurrency() : "TRY";

            // Her portföyün değerini TRY'ye çevir
            BigDecimal exchangeRate = tcmbService.getExchangeRate(currency);

            BigDecimal valueInTRY = currency.equals("TRY")
                    ? dto.getTotalValue()
                    : dto.getTotalValue().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

            BigDecimal investedInTRY = currency.equals("TRY")
                    ? dto.getTotalInvested()
                    : dto.getTotalInvested().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

            BigDecimal pnlInTRY = currency.equals("TRY")
                    ? dto.getUnrealizedPnL()
                    : dto.getUnrealizedPnL().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

            totalValue = totalValue.add(valueInTRY);
            totalInvested = totalInvested.add(investedInTRY);
            totalUnrealizedPnL = totalUnrealizedPnL.add(pnlInTRY);
        }

        BigDecimal totalPnLPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalPnLPercent = totalUnrealizedPnL
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        List<PortfolioDTO> sortedByPnL = allPortfolios.stream()
                .sorted((p1, p2) -> p2.getPnlPercent().compareTo(p1.getPnlPercent()))
                .collect(Collectors.toList());

        List<PortfolioDTO> topPerformers = sortedByPnL.stream().limit(3).collect(Collectors.toList());
        List<PortfolioDTO> worstPerformers = sortedByPnL.stream()
                .skip(Math.max(0, sortedByPnL.size() - 3))
                .collect(Collectors.toList());

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

    /**
     * Portföyün belirli tarih aralığındaki performans verilerini hesaplar.
     * Her gün için portföy değeri ve getiri yüzdesi hesaplanır.
     */

    @Override
    @Transactional(readOnly = true)
    public PortfolioPerformanceDTO getPortfolioPerformance(Long portfolioId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating portfolio performance for ID: {} from {} to {}", portfolioId, startDate, endDate);

        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        String portfolioCurrency = portfolio.getCurrency() != null ? portfolio.getCurrency() : "TRY";

        BigDecimal currentValue = holdingService.calculateCurrentValue(portfolioId, portfolioCurrency);
        BigDecimal totalInvested = holdingService.calculateTotalInvestment(portfolioId, portfolioCurrency);

        BigDecimal totalReturn = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalReturn = currentValue.subtract(totalInvested)
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        List<PerformanceDataPointDTO> historicalData = generateHistoricalPerformanceData(
                portfolio, startDate, endDate, totalInvested, currentValue, portfolioCurrency
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
     * Günlük performans veri noktalarını oluşturur.
     * İşlem geçmişine göre her gün holding değerleri hesaplanır.
     */

    private List<PerformanceDataPointDTO> generateHistoricalPerformanceData(
            Portfolio portfolio, LocalDate startDate, LocalDate endDate,
            BigDecimal totalInvested, BigDecimal currentValue,
            String portfolioCurrency) {

        log.info("Generating historical performance data from {} to {}", startDate, endDate);

        List<PerformanceDataPointDTO> dataPoints = new ArrayList<>();

        List<PortfolioTransaction> transactions = portfolio.getTransactions().stream()
                .sorted(Comparator.comparing(PortfolioTransaction::getTransactionDate))
                .collect(Collectors.toList());

        log.info("Found {} transactions for portfolio {}", transactions.size(), portfolio.getId());

        if (transactions.isEmpty()) {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                dataPoints.add(PerformanceDataPointDTO.builder()
                        .date(currentDate)
                        .value(BigDecimal.ZERO)
                        .returnPercent(BigDecimal.ZERO)
                        .build());
                currentDate = currentDate.plusDays(1);
            }
            log.info("No transactions - generated {} data points", dataPoints.size());
            return dataPoints;
        }

        Map<Long, BigDecimal> holdingsMap = new HashMap<>();
        LocalDate currentDate = startDate;
        int transactionIndex = 0;

        while (!currentDate.isAfter(endDate)) {
            while (transactionIndex < transactions.size()) {
                PortfolioTransaction tx = transactions.get(transactionIndex);
                LocalDate txDate = tx.getTransactionDate().toLocalDate();

                if (txDate.isAfter(currentDate)) break;

                processTransaction(tx, holdingsMap);
                transactionIndex++;
            }

            // Portfolio currency'sine göre hesapla
            BigDecimal holdingsValue = calculateHoldingsValueAtDate(holdingsMap, currentDate, portfolioCurrency);

            BigDecimal returnPercent = BigDecimal.ZERO;
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                returnPercent = holdingsValue.subtract(totalInvested)
                        .divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            dataPoints.add(PerformanceDataPointDTO.builder()
                    .date(currentDate)
                    .value(holdingsValue.setScale(2, RoundingMode.HALF_UP))
                    .returnPercent(returnPercent)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        log.info("Generated {} historical data points", dataPoints.size());
        return dataPoints;
    }

    /**
     * İşlemi holding haritasına uygular.
     * Alışta miktar artırılır, satışta azaltılır.
     */

    private void processTransaction(PortfolioTransaction tx, Map<Long, BigDecimal> holdingsMap) {
        Long instrumentId = tx.getInstrument().getId();
        BigDecimal quantity = tx.getQuantity();

        if (tx.getTransactionType() == TransactionType.BUY) {
            holdingsMap.merge(instrumentId, quantity, BigDecimal::add);
        } else {
            holdingsMap.merge(instrumentId, quantity.negate(), BigDecimal::add);
            if (holdingsMap.getOrDefault(instrumentId, BigDecimal.ZERO)
                    .compareTo(BigDecimal.ZERO) == 0) {
                holdingsMap.remove(instrumentId);
            }
        }
    }

    /**
     * Belirli bir tarihteki holding değerini portföy currency'sine göre hesaplar.
     */

    private BigDecimal calculateHoldingsValueAtDate(Map<Long, BigDecimal> holdingsMap,
                                                    LocalDate date,
                                                    String portfolioCurrency) {
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Map.Entry<Long, BigDecimal> entry : holdingsMap.entrySet()) {
            Long instrumentId = entry.getKey();
            BigDecimal quantity = entry.getValue();

            // Önce TRY cinsinden fiyat al
            BigDecimal currentPriceInTRY = getCurrentPriceForInstrumentInTRY(instrumentId);

            // Portfolio currency'sine çevir
            BigDecimal currentPrice = tcmbService.convertFromTRY(currentPriceInTRY, portfolioCurrency);

            BigDecimal holdingValue = quantity.multiply(currentPrice);
            totalValue = totalValue.add(holdingValue);
        }

        return totalValue;
    }

    /**
     * Enstrümanın güncel fiyatını TRY cinsinden döner.
     * TRY dışındaki enstrümanlar için kur çevirimi yapılır.
     */

    private BigDecimal getCurrentPriceForInstrumentInTRY(Long instrumentId) {
        try {
            BaseInstrument instrument = instrumentRepository.findById(instrumentId).orElse(null);

            if (instrument == null) {
                log.warn("Instrument not found: {}", instrumentId);
                return BigDecimal.ZERO;
            }

            BigDecimal price = instrumentPriceRepository
                    .findTopByInstrumentOrderByTimestampDesc(instrument)
                    .map(InstrumentPrice::getCurrentPrice)
                    .orElse(BigDecimal.ZERO);

            // Enstrüman TRY değilse kur çevir
            String currency = instrument.getCurrency();
            if (currency != null && !currency.equals("TRY")) {
                BigDecimal exchangeRate = tcmbService.getExchangeRate(currency);
                price = price.multiply(exchangeRate).setScale(6, RoundingMode.HALF_UP);
            }

            return price;

        } catch (Exception e) {
            log.error("Error getting price for instrument {}: {}", instrumentId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }


    private BigDecimal getCurrentPriceForInstrument(Long instrumentId) {
        return getCurrentPriceForInstrumentInTRY(instrumentId);
    }

    /**
     * Kullanıcının tüm portföylerinin toplam güncel değerini TRY cinsinden hesaplar.
     */

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPortfolioValue() {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        List<Portfolio> portfolios = portfolioRepository.findByUserId(currentUserId);
        return portfolios.stream()
                .map(p -> holdingService.calculateCurrentValue(p.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Kullanıcının tüm portföylerinin toplam gerçekleşmemiş kâr/zararını hesaplar.
     */

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalUnrealizedPnL() {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        List<Portfolio> portfolios = portfolioRepository.findByUserId(currentUserId);
        return portfolios.stream()
                .map(p -> holdingService.calculateUnrealizedPnL(p.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Portföyü aktif hale getirir.
     */

    @Override
    @Transactional
    public void activatePortfolio(Long portfolioId) {
        log.info("Activating portfolio ID: {}", portfolioId);
        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        portfolio.setActive(true);
        portfolioRepository.save(portfolio);
        log.info("Portfolio activated: {}", portfolioId);
    }

    /**
     * Portföyü pasif hale getirir.
     */

    @Override
    @Transactional
    public void deactivatePortfolio(Long portfolioId) {
        log.info("Deactivating portfolio ID: {}", portfolioId);
        Portfolio portfolio = getPortfolioEntityWithOwnershipCheck(portfolioId);
        portfolio.setActive(false);
        portfolioRepository.save(portfolio);
        log.info("Portfolio deactivated: {}", portfolioId);
    }

    /**
     * Portföyleri isme göre arar.
     */

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> searchPortfoliosByName(String searchTerm) {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        List<Portfolio> portfolios = portfolioRepository.searchByName(currentUserId, searchTerm);
        return portfolios.stream()
                .map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p))
                .collect(Collectors.toList());
    }

    /**
     * Portföyleri türe göre filtreler.
     */

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getPortfoliosByType(String portfolioType) {
        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        PortfolioType type = PortfolioType.valueOf(portfolioType.toUpperCase());
        List<Portfolio> portfolios = portfolioRepository.findByUserIdAndPortfolioType(currentUserId, type);
        return portfolios.stream()
                .map(p -> enrichPortfolioDTO(portfolioMapper.toDTO(p), p))
                .collect(Collectors.toList());
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Portföy sahipliğini doğrular. Yetkisiz erişimde exception fırlatır.
     */

    private Portfolio getPortfolioEntityWithOwnershipCheck(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        if (!portfolio.getUserId().equals(currentUserId)) {
            log.error("Unauthorized access attempt to portfolio: {} by user: {}", portfolioId, currentUserId);
            throw new UnauthorizedException("You don't have permission to access this portfolio");
        }

        return portfolio;
    }

    /**
     * PortfolioDTO'yu güncel değer, yatırım ve kâr/zarar bilgileriyle zenginleştirir.
     */

    private PortfolioDTO enrichPortfolioDTO(PortfolioDTO dto, Portfolio portfolio) {
        String currency = portfolio.getCurrency() != null ? portfolio.getCurrency() : "TRY";

        BigDecimal totalInvested = holdingService.calculateTotalInvestment(portfolio.getId(), currency);
        BigDecimal holdingsValue = holdingService.calculateCurrentValue(portfolio.getId(), currency);

        BigDecimal unrealizedPnL = holdingsValue.subtract(totalInvested);

        BigDecimal pnlPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            pnlPercent = unrealizedPnL
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        dto.setTotalValue(holdingsValue);
        dto.setTotalInvested(totalInvested);
        dto.setUnrealizedPnL(unrealizedPnL);
        dto.setPnlPercent(pnlPercent);
        dto.setHoldingCount(portfolio.getHoldings().size());

        return dto;
    }

    /**
     * Tüm portföylerin varlık dağılımını hesaplar.
     * Her enstrüman türü için toplam değer ve yüzde hesaplanır.
     */

    private List<AssetAllocationDTO> calculateAggregateAssetAllocation(List<Portfolio> portfolios) {
        log.debug("Calculating aggregate asset allocation for {} portfolios", portfolios.size());

        Map<String, AssetAllocationDTO> allocationMap = new HashMap<>();

        for (Portfolio portfolio : portfolios) {
            List<PortfolioHolding> holdings = holdingRepository.findByPortfolioId(portfolio.getId());

            for (PortfolioHolding holding : holdings) {
                String instrumentType = holding.getInstrument().getInstrumentType().name();

                // TRY cinsinden fiyat al
                BigDecimal currentPriceInTRY = getCurrentPriceForInstrumentInTRY(holding.getInstrument().getId());
                BigDecimal currentValue = holding.getQuantity().multiply(currentPriceInTRY);

                if (allocationMap.containsKey(instrumentType)) {
                    AssetAllocationDTO existing = allocationMap.get(instrumentType);
                    existing.setTotalValue(existing.getTotalValue().add(currentValue));
                    existing.setCount(existing.getCount() + 1);
                } else {
                    allocationMap.put(instrumentType, AssetAllocationDTO.builder()
                            .instrumentType(instrumentType)
                            .totalValue(currentValue)
                            .count(1)
                            .percentage(BigDecimal.ZERO)
                            .build());
                }
            }
        }

        BigDecimal grandTotal = allocationMap.values().stream()
                .map(AssetAllocationDTO::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            for (AssetAllocationDTO allocation : allocationMap.values()) {
                BigDecimal percentage = allocation.getTotalValue()
                        .divide(grandTotal, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
                allocation.setPercentage(percentage);
            }
        }

        return allocationMap.values().stream()
                .sorted((a, b) -> b.getTotalValue().compareTo(a.getTotalValue()))
                .collect(Collectors.toList());
    }
}