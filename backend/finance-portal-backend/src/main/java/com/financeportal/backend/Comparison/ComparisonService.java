package com.financeportal.backend.Comparison;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Entity.PriceHistory;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ComparisonService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final PriceHistoryRepository historyRepository;

    /**
     * İki enstrümanı belirtilen zaman dilimine göre karşılaştırır.
     * Anlık fiyatlar, tarihsel veriler ve performans metrikleri hesaplanır.
     */
    public ComparisonDTO compareInstruments(Long id1, Long id2, String period) {

        // Period'a göre tarih aralığını hesapla
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(period);

        log.info("Comparing instruments {} vs {} for period: {} ({} to {})",
                id1, id2, period, startDate, endDate);

        // Enstrümanları getir
        BaseInstrument inst1 = instrumentRepository.findById(id1)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + id1));
        BaseInstrument inst2 = instrumentRepository.findById(id2)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + id2));

        // Anlık fiyatları getir
        InstrumentPrice price1 = priceRepository
                .findTopByInstrumentOrderByTimestampDesc(inst1)
                .orElse(null);
        InstrumentPrice price2 = priceRepository
                .findTopByInstrumentOrderByTimestampDesc(inst2)
                .orElse(null);

        // Tarihsel verileri getir
        List<PriceHistory> history1 = historyRepository
                .findByInstrumentAndDateBetweenOrderByDateAsc(inst1, startDate, endDate);
        List<PriceHistory> history2 = historyRepository
                .findByInstrumentAndDateBetweenOrderByDateAsc(inst2, startDate, endDate);

        log.info("Found {} data points for inst1, {} for inst2",
                history1.size(), history2.size());

        // Tarihleri birleştir (her iki enstrüman için ortak tarihler)
        List<ComparisonDTO.ComparisonDataPoint> dataPoints = mergeHistoricalData(history1, history2);

        // Performans metrikleri hesapla
        ComparisonDTO.PerformanceMetrics metrics = ComparisonDTO.PerformanceMetrics.builder()
                .instrument1Metrics(calculateMetrics(history1, price1))
                .instrument2Metrics(calculateMetrics(history2, price2))
                .build();

        return ComparisonDTO.builder()
                .instrument1(buildInstrumentInfo(inst1, price1))
                .instrument2(buildInstrumentInfo(inst2, price2))
                .historicalData(dataPoints)
                .metrics(metrics)
                .period(period)
                .build();
    }

    /**
     * Belirtilen period string'ine göre başlangıç tarihini hesaplar.
     * 1H=1 hafta, 1A=1 ay, 3A=3 ay, 6A=6 ay, 1Y=1 yıl
     */

    private LocalDate calculateStartDate(String period) {
        LocalDate now = LocalDate.now();

        return switch (period.toUpperCase()) {
            case "1H" -> now.minusWeeks(1);
            case "1A" -> now.minusMonths(1);
            case "3A" -> now.minusMonths(3);
            case "6A" -> now.minusMonths(6);
            case "1Y" -> now.minusYears(1);
            default -> now.minusMonths(1);
        };
    }

    /**
     * İki enstrümanın tarihsel verilerini ortak tarihler üzerinden birleştirir.
     * Sadece her iki enstrümanda da mevcut olan tarihler dahil edilir.
     */

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

    /**
     * Dönem bazlı performans metriklerini hesaplar.
     * Dönem değişimi, volatilite, en yüksek/düşük fiyat ve fiyat aralığı içerir.
     */

    private ComparisonDTO.MetricData calculateMetrics(
            List<PriceHistory> history,
            InstrumentPrice currentPrice) {

        // Dönem değişimi (ilk fiyat → son fiyat)
        BigDecimal periodChange = BigDecimal.ZERO;
        if (!history.isEmpty()) {
            BigDecimal firstPrice = history.get(0).getClose();  // Dönem başı
            BigDecimal latestPrice = currentPrice != null
                    ? currentPrice.getCurrentPrice()
                    : history.get(history.size() - 1).getClose();  // Dönem sonu
            periodChange = calculatePercentChange(firstPrice, latestPrice);
        }

        // Volatilite
        BigDecimal volatility = calculateVolatility(history);

        // En yüksek/düşük (currentPrice'ı da dahil et)
        BigDecimal highest = history.stream()
                .map(PriceHistory::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (currentPrice != null && currentPrice.getHighPrice() != null) {
            highest = highest.max(currentPrice.getHighPrice());
        }

        BigDecimal lowest = history.stream()
                .map(PriceHistory::getLow)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (currentPrice != null && currentPrice.getLowPrice() != null) {
            if (lowest.compareTo(BigDecimal.ZERO) == 0) {
                lowest = currentPrice.getLowPrice();
            } else {
                lowest = lowest.min(currentPrice.getLowPrice());
            }
        }

        BigDecimal priceRange = highest.subtract(lowest);

        return ComparisonDTO.MetricData.builder()
                .periodChange(periodChange)
                .volatility(volatility)
                .highestPrice(highest)
                .lowestPrice(lowest)
                .priceRange(priceRange)
                .build();
    }

    /**
     * İki fiyat arasındaki yüzde değişimini hesaplar.
     * Formül: ((yeni - eski) / eski) × 100
     */

    private BigDecimal calculatePercentChange(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return newPrice.subtract(oldPrice)
                .divide(oldPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Günlük getirilerin standart sapmasını hesaplayarak volatiliteyi döner.
     * En az 2 veri noktası gerektirir, aksi halde sıfır döner.
     */

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

    /**
     * Enstrüman ve fiyat bilgisinden karşılaştırma için özet bilgi oluşturur.
     */

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
