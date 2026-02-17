package com.financeportal.backend.Instrument.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Instrument.DTO.External.AlphaVantagePriceDTO;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Entity.PreciousInstrument;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlphaVantageService {

    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // ✅ Desteklenen semboller
    // AlphaVantage formatı → DB sembolü, isim, metalType, unit
    private static final List<Map<String, String>> PRECIOUS_METALS = List.of(
            Map.of("avSymbol", "XAUUSD", "dbSymbol", "XAU/USD",
                    "name", "Altın (Ons)", "metalType", "GOLD", "unit", "oz"),
            Map.of("avSymbol", "XAGUSD", "dbSymbol", "XAG/USD",
                    "name", "Gümüş (Ons)", "metalType", "SILVER", "unit", "oz"),
            Map.of("avSymbol", "XPTUSD", "dbSymbol", "XPT/USD",
                    "name", "Platin (Ons)", "metalType", "PLATINUM", "unit", "oz"),
            Map.of("avSymbol", "XPDUSD", "dbSymbol", "XPD/USD",
                    "name", "Paladyum (Ons)", "metalType", "PALLADIUM", "unit", "oz")
    );

    // ✅ Tek sembol için fiyat çek
    public InstrumentPrice fetchQuote(String avSymbol, String dbSymbol,
                                      String name, String metalType, String unit) {
        try {
            String cacheKey = "alphavantage:quote:" + dbSymbol;

            // ✅ Redis'ten oku
            AlphaVantagePriceDTO cachedDTO = (AlphaVantagePriceDTO) redisTemplate
                    .opsForValue().get(cacheKey);

            if (cachedDTO != null) {
                log.info("✅ Cache HIT - AlphaVantage: {}", dbSymbol);
                return convertToEntity(cachedDTO);
            }

            log.info("🔍 Cache MISS - Fetching AlphaVantage: {}", avSymbol);

            // ✅ AlphaVantage GLOBAL_QUOTE endpoint
            String url = apiUrl + "?function=GLOBAL_QUOTE" +
                    "&symbol=" + avSymbol +
                    "&apikey=" + apiKey;

            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isEmpty()) {
                log.warn("Empty response for: {}", avSymbol);
                return null;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode quote = root.path("Global Quote");

            if (quote.isEmpty() || quote.isMissingNode()) {
                log.warn("No Global Quote data for: {}", avSymbol);
                log.debug("Response: {}", response);
                return null;
            }

            // ✅ AlphaVantage field'ları
            String priceStr    = quote.path("05. price").asText();
            String openStr     = quote.path("02. open").asText();
            String highStr     = quote.path("03. high").asText();
            String lowStr      = quote.path("04. low").asText();
            String prevStr     = quote.path("08. previous close").asText();

            if (priceStr.isEmpty() || priceStr.equals("0.0000")) {
                log.warn("Invalid price for: {}", avSymbol);
                return null;
            }

            BigDecimal currentPrice  = new BigDecimal(priceStr);
            BigDecimal openPrice     = new BigDecimal(openStr);
            BigDecimal highPrice     = new BigDecimal(highStr);
            BigDecimal lowPrice      = new BigDecimal(lowStr);
            BigDecimal previousClose = new BigDecimal(prevStr);

            // ✅ Enstrümanı bul veya oluştur
            BaseInstrument instrument = instrumentRepository
                    .findBySymbol(dbSymbol)
                    .orElseGet(() -> createPreciousInstrument(
                            dbSymbol, name, metalType, unit
                    ));

            InstrumentPrice price = InstrumentPrice.builder()
                    .instrument(instrument)
                    .currentPrice(currentPrice)
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .previousClose(previousClose)
                    .timestamp(LocalDateTime.now())
                    .build();

            priceRepository.save(price);

            // ✅ Redis'e kaydet (1 saat TTL - günlük 25 limit koruması)
            AlphaVantagePriceDTO dto = AlphaVantagePriceDTO.builder()
                    .symbol(dbSymbol)
                    .currentPrice(currentPrice)
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .previousClose(previousClose)
                    .timestamp(LocalDateTime.now())
                    .build();

            redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofHours(1));
            log.info("✅ Cached AlphaVantage: {} (TTL: 1saat)", dbSymbol);

            log.info("✅ AlphaVantage: {} = {} (prev: {})",
                    dbSymbol, currentPrice, previousClose);

            return price;

        } catch (Exception e) {
            log.error("❌ AlphaVantage error for {}: {}", avSymbol, e.getMessage());
            return null;
        }
    }

    // ✅ Tüm kıymetli metalleri güncelle
    public int updatePreciousMetals() {
        log.info("📊 Updating precious metals via AlphaVantage...");
        int updated = 0;

        for (Map<String, String> metal : PRECIOUS_METALS) {
            try {
                InstrumentPrice price = fetchQuote(
                        metal.get("avSymbol"),
                        metal.get("dbSymbol"),
                        metal.get("name"),
                        metal.get("metalType"),
                        metal.get("unit")
                );

                if (price != null) {
                    updated++;
                    log.info("Updated {}/{}: {}",
                            updated, PRECIOUS_METALS.size(), metal.get("dbSymbol"));
                }

                // ✅ Rate limit: 25 istek/gün → her istek arası 15sn bekle
                Thread.sleep(15000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("❌ Failed: {}", metal.get("dbSymbol"));
            }
        }

        log.info("✅ Precious metals updated: {}/{}", updated, PRECIOUS_METALS.size());
        return updated;
    }

    // ✅ DTO → Entity
    private InstrumentPrice convertToEntity(AlphaVantagePriceDTO dto) {
        BaseInstrument instrument = instrumentRepository
                .findBySymbol(dto.getSymbol())
                .orElseThrow(() -> new RuntimeException(
                        "Instrument not found: " + dto.getSymbol()
                ));

        return InstrumentPrice.builder()
                .instrument(instrument)
                .currentPrice(dto.getCurrentPrice())
                .openPrice(dto.getOpenPrice())
                .highPrice(dto.getHighPrice())
                .lowPrice(dto.getLowPrice())
                .previousClose(dto.getPreviousClose())
                .timestamp(dto.getTimestamp())
                .build();
    }

    // ✅ PreciousInstrument oluştur
    private BaseInstrument createPreciousInstrument(String symbol, String name,
                                                    String metalType, String unit) {
        PreciousInstrument precious = PreciousInstrument.builder()
                .symbol(symbol)
                .name(name)
                .metalType(metalType)
                .unit(unit)
                .exchange("FOREX")
                .currency("USD")
                .active(true)
                .build();

        instrumentRepository.save(precious);
        log.info("✅ Auto-created PreciousInstrument: {}", symbol);
        return precious;
    }
}
