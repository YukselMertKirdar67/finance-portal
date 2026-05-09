package com.financeportal.backend.Home;

import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.Service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HomeService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final NewsService newsService;

    /**
     * Anasayfa için piyasa özeti, kazananlar, kaybedenler,
     * son haberler, istatistikler ve kategori özetlerini tek seferde getirir.
     * Sonuçlar Redis cache'te tutulur.
     */

    @Cacheable(value = "homePage", key = "'data'")
    public HomePageDTO getHomePageData() {
        log.info("🏠 Generating home page data...");

        return HomePageDTO.builder()
                .marketOverview(getMarketOverview())
                .topGainers(getTopGainers(5))
                .topLosers(getTopLosers(5))
                .recentNews(getRecentNews(5))
                .marketStats(getMarketStats())
                .categories(getCategorySummaries())
                .build();
    }

    /**
     *  Piyasa Özeti (4 önemli enstrüman)
     */
    private List<HomePageDTO.MarketOverviewItem> getMarketOverview() {
        // USD/TRY, EUR/TRY, BTC, Altın
        List<String> symbols = List.of("USD/TRY", "EUR/TRY", "BINANCE:BTCUSDT", "XAU/USD");

        List<HomePageDTO.MarketOverviewItem> overview = new ArrayList<>();

        for (String symbol : symbols) {
            instrumentRepository.findBySymbol(symbol).ifPresent(instrument -> {
                priceRepository.findTopByInstrumentOrderByTimestampDesc(instrument)
                        .ifPresent(price -> {
                            overview.add(buildMarketOverviewItem(instrument, price));
                        });
            });
        }

        return overview;
    }

    /**
     * En Çok Kazananlar
     */
    private List<HomePageDTO.InstrumentSummary> getTopGainers(int limit) {
        List<BaseInstrument> instruments = instrumentRepository.findByActiveTrue();

        return instruments.stream()
                .map(instrument -> {
                    InstrumentPrice price = priceRepository
                            .findTopByInstrumentOrderByTimestampDesc(instrument)
                            .orElse(null);
                    return price != null ? buildInstrumentSummary(instrument, price) : null;
                })
                .filter(Objects::nonNull)
                .filter(item -> item.getIsPositive())
                .sorted((a, b) -> {
                    // ChangePercent'e göre sırala (yüksekten düşüğe)
                    String aPercent = a.getChangePercent().replace("%", "").replace("+", "");
                    String bPercent = b.getChangePercent().replace("%", "").replace("+", "");
                    return Double.compare(
                            Double.parseDouble(bPercent),
                            Double.parseDouble(aPercent)
                    );
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * En Çok Kaybedenler
     */
    private List<HomePageDTO.InstrumentSummary> getTopLosers(int limit) {
        List<BaseInstrument> instruments = instrumentRepository.findByActiveTrue();

        return instruments.stream()
                .map(instrument -> {
                    InstrumentPrice price = priceRepository
                            .findTopByInstrumentOrderByTimestampDesc(instrument)
                            .orElse(null);
                    return price != null ? buildInstrumentSummary(instrument, price) : null;
                })
                .filter(Objects::nonNull)
                .filter(item -> !item.getIsPositive())
                .sorted((a, b) -> {
                    // ChangePercent'e göre sırala (düşükten yükseğe)
                    String aPercent = a.getChangePercent().replace("%", "").replace("-", "");
                    String bPercent = b.getChangePercent().replace("%", "").replace("-", "");
                    return Double.compare(
                            Double.parseDouble(aPercent),
                            Double.parseDouble(bPercent)
                    );
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Son Haberler
     */
    private List<NewsResponseDTO> getRecentNews(int limit) {
        try {
            return newsService.getAllNews(0, limit).getContent();
        } catch (Exception e) {
            log.error("Error fetching recent news: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Piyasa İstatistikleri
     */
    private HomePageDTO.MarketStats getMarketStats() {
        List<BaseInstrument> instruments = instrumentRepository.findByActiveTrue();

        int rising = 0;
        int falling = 0;
        int unchanged = 0;

        for (BaseInstrument instrument : instruments) {
            InstrumentPrice price = priceRepository
                    .findTopByInstrumentOrderByTimestampDesc(instrument)
                    .orElse(null);

            if (price != null && price.getChangePercent() != null) {
                BigDecimal change = price.getChangePercent();
                if (change.compareTo(BigDecimal.ZERO) > 0) {
                    rising++;
                } else if (change.compareTo(BigDecimal.ZERO) < 0) {
                    falling++;
                } else {
                    unchanged++;
                }
            }
        }

        return HomePageDTO.MarketStats.builder()
                .rising(rising)
                .falling(falling)
                .unchanged(unchanged)
                .build();
    }

    /**
     * Kategori Özetleri
     */
    private List<HomePageDTO.CategorySummary> getCategorySummaries() {
        Map<InstrumentType, Long> counts = new HashMap<>();

        for (InstrumentType type : InstrumentType.values()) {
            // Entity class kullan
            Class<? extends BaseInstrument> entityClass = switch (type) {
                case FOREX -> ForexInstrument.class;
                case STOCK -> StockInstrument.class;
                case CRYPTO -> CryptoInstrument.class;
                case BOND -> BondInstrument.class;
                case PRECIOUS -> PreciousInstrument.class;
                case FUND -> FundInstrument.class;
            };

            long count = instrumentRepository.countByType(entityClass);
            if (count > 0) {
                counts.put(type, count);
            }
        }

        return counts.entrySet().stream()
                .map(entry -> HomePageDTO.CategorySummary.builder()
                        .type(entry.getKey().name())
                        .displayName(getDisplayName(entry.getKey()))
                        .count(entry.getValue().intValue())
                        .iconName(getIconName(entry.getKey()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Enstrüman ve fiyat bilgisinden piyasa özeti öğesi oluşturur.
     * Anasayfadaki piyasa özeti kartları için kullanılır.
     */

    private HomePageDTO.MarketOverviewItem buildMarketOverviewItem(
            BaseInstrument instrument,
            InstrumentPrice price
    ) {
        return HomePageDTO.MarketOverviewItem.builder()
                .id(instrument.getId())
                .symbol(instrument.getSymbol())
                .name(instrument.getName())
                .currentPrice(formatPrice(price.getCurrentPrice()))
                .change(formatChange(price.getChangeAmount()))
                .changePercent(formatPercent(price.getChangePercent()))
                .isPositive(price.getChangePercent() != null &&
                        price.getChangePercent().compareTo(BigDecimal.ZERO) >= 0)
                .build();
    }

    /**
     * Enstrüman ve fiyat bilgisinden enstrüman özeti oluşturur.
     * En çok kazananlar ve kaybedenler listesi için kullanılır.
     */

    private HomePageDTO.InstrumentSummary buildInstrumentSummary(
            BaseInstrument instrument,
            InstrumentPrice price
    ) {
        return HomePageDTO.InstrumentSummary.builder()
                .id(instrument.getId())
                .symbol(instrument.getSymbol())
                .name(instrument.getName())
                .currentPrice(formatPrice(price.getCurrentPrice()))
                .change(formatChange(price.getChangeAmount()))
                .changePercent(formatPercent(price.getChangePercent()))
                .isPositive(price.getChangePercent() != null &&
                        price.getChangePercent().compareTo(BigDecimal.ZERO) >= 0)
                .type(instrument.getInstrumentType().name())
                .build();
    }

    /**
     * Fiyatı formatlar. Null gelirse "0.00" döner.
     */

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0.00";
        return price.setScale(2, RoundingMode.HALF_UP).toString();
    }

    /**
     * Değişim miktarını formatlar. Pozitifse "+" prefix ekler.
     */

    private String formatChange(BigDecimal change) {
        if (change == null) return "0.00";
        String formatted = change.setScale(2, RoundingMode.HALF_UP).toString();
        return change.compareTo(BigDecimal.ZERO) > 0 ? "+" + formatted : formatted;
    }

    /**
     * Yüzde değerini formatlar. Pozitifse "+" prefix, sona "%" ekler.
     */

    private String formatPercent(BigDecimal percent) {
        if (percent == null) return "0.00%";
        String formatted = percent.setScale(2, RoundingMode.HALF_UP).toString();
        String withSign = percent.compareTo(BigDecimal.ZERO) > 0 ? "+" + formatted : formatted;
        return withSign + "%";
    }

    /**
     * Enstrüman türünün Türkçe görünen adını döner.
     */

    private String getDisplayName(InstrumentType type) {
        return switch (type) {
            case FOREX -> "Döviz";
            case STOCK -> "Hisse Senedi";
            case CRYPTO -> "Kripto";
            case BOND -> "Tahvil";
            case PRECIOUS -> "Kıymetli Maden";
            case FUND -> "Yatırım Fonu";
        };
    }

    /**
     * Enstrüman türüne ait ikon adını döner (Lucide React ikon isimleri).
     */

    private String getIconName(InstrumentType type) {
        return switch (type) {
            case FOREX -> "DollarSign";
            case STOCK -> "TrendingUp";
            case CRYPTO -> "Bitcoin";
            case BOND -> "FileText";
            case PRECIOUS -> "Gem";
            case FUND -> "Briefcase";
        };
    }
}