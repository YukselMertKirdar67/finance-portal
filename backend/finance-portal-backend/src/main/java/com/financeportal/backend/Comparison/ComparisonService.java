package com.financeportal.backend.Comparison;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Entity.PriceHistory;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComparisonService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final PriceHistoryRepository historyRepository;

    public ComparisonDTO compareInstruments(Long id1, Long id2, LocalDate startDate, LocalDate endDate) {

        // ✅ Enstrümanları getir
        BaseInstrument inst1 = instrumentRepository.findById(id1)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + id1));
        BaseInstrument inst2 = instrumentRepository.findById(id2)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + id2));

        // ✅ Anlık fiyatları getir
        InstrumentPrice price1 = priceRepository
                .findTopByInstrumentOrderByTimestampDesc(inst1)
                .orElse(null);
        InstrumentPrice price2 = priceRepository
                .findTopByInstrumentOrderByTimestampDesc(inst2)
                .orElse(null);

        // ✅ Tarihsel verileri getir
        List<PriceHistory> history1 = historyRepository
                .findByInstrumentAndDateBetweenOrderByDateAsc(inst1, startDate, endDate);
        List<PriceHistory> history2 = historyRepository
                .findByInstrumentAndDateBetweenOrderByDateAsc(inst2, startDate, endDate);

        // ✅ Tarihleri birleştir (her iki enstrüman için ortak tarihler)
        List<ComparisonDTO.ComparisonDataPoint> dataPoints = mergeHistoricalData(history1, history2);

        // ✅ Performans metrikleri hesapla
        ComparisonDTO.PerformanceMetrics metrics = ComparisonDTO.PerformanceMetrics.builder()
                .instrument1Metrics(calculateMetrics(history1, price1))
                .instrument2Metrics(calculateMetrics(history2, price2))
                .build();

        return ComparisonDTO.builder()
                .instrument1(buildInstrumentInfo(inst1, price1))
                .instrument2(buildInstrumentInfo(inst2, price2))
                .historicalData(dataPoints)
                .metrics(metrics)
                .build();
    }

    // ✅ İki enstrümanın tarihsel verilerini birleştir
    private List<ComparisonDTO.ComparisonDataPoint> mergeHistoricalData(
            List<PriceHistory> history1,
            List<PriceHistory> history2) {

        List<ComparisonDTO.ComparisonDataPoint> result = new ArrayList<>();

        for (PriceHistory h1 : history1) {
            LocalDate date = h1.getDate();

            // Aynı tarihteki ikinci enstrümanın verisini bul
            history2.stream()
                    .filter(h2 -> h2.getDate().equals(date))
                    .findFirst()
                    .ifPresent(h2 -> {
                        result.add(ComparisonDTO.ComparisonDataPoint.builder()
                                .date(date)
                                .price1(h1.getClose())
                                .price2(h2.getClose())
                                .build());
                    });
        }

        return result;
    }

    // ✅ Performans metrikleri hesapla
    private ComparisonDTO.MetricData calculateMetrics(
            List<PriceHistory> history,
            InstrumentPrice currentPrice) {

        if (history.isEmpty()) {
            return ComparisonDTO.MetricData.builder()
                    .dailyChange(BigDecimal.ZERO)
                    .weeklyChange(BigDecimal.ZERO)
                    .monthlyChange(BigDecimal.ZERO)
                    .volatility(BigDecimal.ZERO)
                    .highestPrice(BigDecimal.ZERO)
                    .lowestPrice(BigDecimal.ZERO)
                    .priceRange(BigDecimal.ZERO)
                    .build();
        }

        // ✅ En son fiyat
        BigDecimal latestPrice = currentPrice != null
                ? currentPrice.getCurrentPrice()
                : history.get(history.size() - 1).getClose();

        // ✅ Günlük değişim (son 1 gün)
        BigDecimal dailyChange = BigDecimal.ZERO;
        if (history.size() >= 2) {
            BigDecimal yesterday = history.get(history.size() - 2).getClose();
            dailyChange = calculatePercentChange(yesterday, latestPrice);
        }

        // ✅ Haftalık değişim (son 7 gün)
        BigDecimal weeklyChange = BigDecimal.ZERO;
        if (history.size() >= 7) {
            BigDecimal weekAgo = history.get(history.size() - 7).getClose();
            weeklyChange = calculatePercentChange(weekAgo, latestPrice);
        }

        // ✅ Aylık değişim (son 30 gün veya tüm veri)
        BigDecimal monthlyChange = BigDecimal.ZERO;
        if (!history.isEmpty()) {
            BigDecimal firstPrice = history.get(0).getClose();
            monthlyChange = calculatePercentChange(firstPrice, latestPrice);
        }

        // ✅ Volatilite (standart sapma)
        BigDecimal volatility = calculateVolatility(history);

        // ✅ En yüksek/düşük
        BigDecimal highest = history.stream()
                .map(PriceHistory::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowest = history.stream()
                .map(PriceHistory::getLow)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal priceRange = highest.subtract(lowest);

        return ComparisonDTO.MetricData.builder()
                .dailyChange(dailyChange)
                .weeklyChange(weeklyChange)
                .monthlyChange(monthlyChange)
                .volatility(volatility)
                .highestPrice(highest)
                .lowestPrice(lowest)
                .priceRange(priceRange)
                .build();
    }

    // ✅ Yüzde değişim hesapla
    private BigDecimal calculatePercentChange(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return newPrice.subtract(oldPrice)
                .divide(oldPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ✅ Volatilite hesapla (standart sapma)
    private BigDecimal calculateVolatility(List<PriceHistory> history) {
        if (history.size() < 2) return BigDecimal.ZERO;

        // Günlük getiri oranlarını hesapla
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            BigDecimal prev = history.get(i - 1).getClose();
            BigDecimal curr = history.get(i).getClose();
            BigDecimal dailyReturn = calculatePercentChange(prev, curr);
            returns.add(dailyReturn);
        }

        // Ortalama
        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 4, RoundingMode.HALF_UP);

        // Varyans
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 4, RoundingMode.HALF_UP);

        // Standart sapma
        double volatilityValue = Math.sqrt(variance.doubleValue());
        return BigDecimal.valueOf(volatilityValue).setScale(2, RoundingMode.HALF_UP);
    }

    // ✅ InstrumentInfo oluştur
    private ComparisonDTO.InstrumentInfo buildInstrumentInfo(
            BaseInstrument instrument,
            InstrumentPrice price) {

        return ComparisonDTO.InstrumentInfo.builder()
                .id(instrument.getId())
                .symbol(instrument.getSymbol())
                .name(instrument.getName())
                .type(instrument.getInstrumentType().toString())
                .currency(instrument.getCurrency())
                .currentPrice(price != null ? price.getCurrentPrice() : BigDecimal.ZERO)
                .build();
    }
}
