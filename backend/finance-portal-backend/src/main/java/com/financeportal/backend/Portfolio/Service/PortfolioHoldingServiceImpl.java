package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Service.TcmbService;
import com.financeportal.backend.Portfolio.DTO.AssetAllocationDTO;
import com.financeportal.backend.Portfolio.DTO.HoldingDTO;
import com.financeportal.backend.Portfolio.Entity.PortfolioHolding;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapper;
import com.financeportal.backend.Portfolio.Repository.PortfolioHoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class PortfolioHoldingServiceImpl implements PortfolioHoldingService {

    private final PortfolioHoldingRepository holdingRepository;
    private final InstrumentPriceRepository priceRepository;
    private final PortfolioCalculationService calculationService;
    private final PortfolioMapper portfolioMapper;
    private final TcmbService tcmbService;

    /**
     * Portföydeki tüm holdingleri portföy currency'sine göre hesaplayarak getirir.
     */
    @Override
    @Transactional(readOnly = true)
    public List<HoldingDTO> getHoldingsByPortfolioId(Long portfolioId, String portfolioCurrency) {
        log.info("Fetching holdings for portfolio ID: {} (currency: {})", portfolioId, portfolioCurrency);

        List<PortfolioHolding> holdings = holdingRepository.findByPortfolioIdWithInstrument(portfolioId);

        return holdings.stream()
                .map(h -> enrichHoldingDTO(h, portfolioCurrency))
                .collect(Collectors.toList());
    }

    /**
     * Portföydeki toplam yatırım tutarını portföy currency'sine çevirerek hesaplar.
     * Holding'lerin ortalama alış fiyatları TRY'ye çevrilip toplanır.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalInvestment(Long portfolioId, String portfolioCurrency) {
        log.debug("Calculating total investment for portfolio ID: {} (currency: {})", portfolioId, portfolioCurrency);

        List<PortfolioHolding> holdings = holdingRepository.findByPortfolioId(portfolioId);

        BigDecimal totalInTRY = holdings.stream()
                .map(h -> {
                    BigDecimal avgPriceInTRY = h.getAverageBuyPrice();
                    String currency = h.getCurrency();
                    BigDecimal exchangeRate = h.getExchangeRate();

                    if (currency != null && !currency.equals("TRY")
                            && exchangeRate != null
                            && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                        avgPriceInTRY = h.getAverageBuyPrice()
                                .multiply(exchangeRate)
                                .setScale(6, RoundingMode.HALF_UP);
                    }

                    return calculationService.calculateTotalInvestment(h.getQuantity(), avgPriceInTRY);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return tcmbService.convertFromTRY(totalInTRY, portfolioCurrency);
    }

    /**
     * Portföydeki toplam güncel değeri portföy currency'sine çevirerek hesaplar.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateCurrentValue(Long portfolioId, String portfolioCurrency) {
        log.debug("Calculating current value for portfolio ID: {} (currency: {})", portfolioId, portfolioCurrency);

        List<PortfolioHolding> holdings = holdingRepository.findByPortfolioIdWithInstrument(portfolioId);

        BigDecimal totalInTRY = holdings.stream()
                .map(h -> {
                    BigDecimal currentPrice = getCurrentPriceInTRY(h.getInstrument());
                    return calculationService.calculateCurrentValue(h.getQuantity(), currentPrice);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return tcmbService.convertFromTRY(totalInTRY, portfolioCurrency);
    }

    /**
     * Portföydeki tüm holdingleri TRY cinsinden getirir (geriye uyumluluk).
     */
    @Override
    @Transactional(readOnly = true)
    public List<HoldingDTO> getHoldingsByPortfolioId(Long portfolioId) {
        return getHoldingsByPortfolioId(portfolioId, "TRY");
    }

    /**
     * ID'ye göre holding getirir.
     */
    @Override
    @Transactional(readOnly = true)
    public HoldingDTO getHoldingById(Long holdingId) {
        log.info("Fetching holding by ID: {}", holdingId);

        PortfolioHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding not found with id: " + holdingId));

        return enrichHoldingDTO(holding, "TRY");
    }

    /**
     * Portföy ve enstrüman ID'sine göre holding getirir.
     */
    @Override
    @Transactional(readOnly = true)
    public HoldingDTO getHoldingByPortfolioAndInstrument(Long portfolioId, Long instrumentId) {
        log.info("Fetching holding for portfolio: {} and instrument: {}", portfolioId, instrumentId);

        Optional<PortfolioHolding> holdingOpt = holdingRepository
                .findByPortfolioIdAndInstrumentId(portfolioId, instrumentId);

        return holdingOpt.map(h -> enrichHoldingDTO(h, "TRY")).orElse(null);
    }

    /**
     * Portföydeki aktif (miktarı sıfırdan büyük) holdingleri getirir.
     */
    @Override
    @Transactional(readOnly = true)
    public List<HoldingDTO> getActiveHoldings(Long portfolioId) {
        log.info("Fetching active holdings for portfolio ID: {}", portfolioId);

        List<PortfolioHolding> holdings = holdingRepository.findActiveHoldingsByPortfolioId(portfolioId);

        return holdings.stream()
                .map(h -> enrichHoldingDTO(h, "TRY"))
                .collect(Collectors.toList());
    }

    /**
     * Portföydeki en yüksek değerli N holdingi getirir.
     */
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

    /**
     * Portföydeki varlık dağılımını portföy currency'sine göre hesaplar.
     * Enstrüman türlerine göre gruplar ve yüzde hesaplar.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AssetAllocationDTO> getAssetAllocation(Long portfolioId, String portfolioCurrency) {
        log.info("Calculating asset allocation for portfolio ID: {} (currency: {})", portfolioId, portfolioCurrency);

        List<HoldingDTO> holdings = getHoldingsByPortfolioId(portfolioId, portfolioCurrency);

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

        allocation.sort(Comparator.comparing(AssetAllocationDTO::getTotalValue).reversed());
        return allocation;
    }

    /**
     * Portföydeki varlık dağılımını TRY cinsinden hesaplar (geriye uyumluluk).
     */
    @Override
    @Transactional(readOnly = true)
    public List<AssetAllocationDTO> getAssetAllocation(Long portfolioId) {
        return getAssetAllocation(portfolioId, "TRY");
    }

    /**
     * Toplam yatırımı TRY cinsinden hesaplar (geriye uyumluluk).
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalInvestment(Long portfolioId) {
        return calculateTotalInvestment(portfolioId, "TRY");
    }

    /**
     * Güncel değeri TRY cinsinden hesaplar (geriye uyumluluk).
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateCurrentValue(Long portfolioId) {
        return calculateCurrentValue(portfolioId, "TRY");
    }

    /**
     * Portföydeki toplam gerçekleşmemiş kâr/zararı hesaplar.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateUnrealizedPnL(Long portfolioId) {
        log.debug("Calculating unrealized P&L for portfolio ID: {}", portfolioId);

        BigDecimal currentValue = calculateCurrentValue(portfolioId);
        BigDecimal totalInvestment = calculateTotalInvestment(portfolioId);

        return currentValue.subtract(totalInvestment);
    }

    /**
     * Belirli bir holding için gerçekleşmemiş kâr/zararı hesaplar.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateHoldingUnrealizedPnL(Long holdingId) {
        log.debug("Calculating unrealized P&L for holding ID: {}", holdingId);

        PortfolioHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding not found with id: " + holdingId));

        BigDecimal currentPrice = getCurrentPriceInTRY(holding.getInstrument());

        return calculationService.calculateUnrealizedPnL(
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                currentPrice
        );
    }

    /**
     * Portföyde belirli bir enstrümana ait holding olup olmadığını kontrol eder.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean holdingExists(Long portfolioId, Long instrumentId) {
        return holdingRepository.existsByPortfolioIdAndInstrumentId(portfolioId, instrumentId);
    }

    /**
     * Holding'i kalıcı olarak siler.
     */
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

    /**
     * Portföydeki sıfır miktarlı holdingleri temizler.
     */
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
     * HoldingDTO'yu güncel fiyat, yatırım değeri ve kâr/zarar bilgileriyle zenginleştirir.
     * Fiyatlar önce TRY'ye çevrilir, sonra portföy currency'sine dönüştürülür.
     */

    private HoldingDTO enrichHoldingDTO(PortfolioHolding holding, String portfolioCurrency) {

        BigDecimal currentPriceInTRY = getCurrentPriceInTRY(holding.getInstrument());
        BigDecimal avgPriceInTRY = holding.getAverageBuyPrice();
        String holdingCurrency = holding.getCurrency();
        BigDecimal exchangeRate = holding.getExchangeRate();

        log.info("DEBUG enrichHoldingDTO - symbol: {}, currency: {}, avgBuyPrice: {}, exchangeRate: {}, currentPriceInTRY: {}, portfolioCurrency: {}",
                holding.getInstrument().getSymbol(), holdingCurrency,
                holding.getAverageBuyPrice(), exchangeRate,
                currentPriceInTRY, portfolioCurrency);

        if (holdingCurrency != null && !holdingCurrency.equals("TRY")
                && exchangeRate != null
                && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
            avgPriceInTRY = holding.getAverageBuyPrice()
                    .multiply(exchangeRate)
                    .setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal currentPrice = tcmbService.convertFromTRY(currentPriceInTRY, portfolioCurrency);
        BigDecimal avgPrice = tcmbService.convertFromTRY(avgPriceInTRY, portfolioCurrency);

        HoldingDTO dto = portfolioMapper.toHoldingDTO(holding, currentPrice);

        BigDecimal totalInvestment = calculationService.calculateTotalInvestment(holding.getQuantity(), avgPrice);
        BigDecimal currentValue = calculationService.calculateCurrentValue(holding.getQuantity(), currentPrice);
        BigDecimal unrealizedPnL = calculationService.calculateUnrealizedPnL(holding.getQuantity(), avgPrice, currentPrice);
        BigDecimal pnlPercent = calculationService.calculatePnLPercent(avgPrice, currentPrice);

        dto.setTotalInvestment(totalInvestment);
        dto.setCurrentValue(currentValue);
        dto.setUnrealizedPnL(unrealizedPnL);
        dto.setPnlPercent(pnlPercent);

        return dto;
    }

    /**
     * Enstrümanın güncel fiyatını TRY cinsinden döner.
     * TRY dışındaki enstrümanlar için TCMB kur çevirimi yapılır.
     */

    private BigDecimal getCurrentPriceInTRY(BaseInstrument instrument) {
        BigDecimal price = priceRepository
                .findTopByInstrumentOrderByTimestampDesc(instrument)
                .map(InstrumentPrice::getCurrentPrice)
                .orElse(BigDecimal.ZERO);

        String currency = instrument.getCurrency();
        if (currency != null && !currency.equals("TRY")) {
            BigDecimal rate = tcmbService.getExchangeRate(currency);
            price = price.multiply(rate).setScale(6, RoundingMode.HALF_UP);
            log.debug("Price converted: {} {} → {} TRY (rate: {})",
                    instrument.getSymbol(), currency, price, rate);
        }

        return price;
    }
}
