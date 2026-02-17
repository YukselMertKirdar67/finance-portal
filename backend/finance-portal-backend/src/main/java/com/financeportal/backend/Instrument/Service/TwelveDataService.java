package com.financeportal.backend.Instrument.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Instrument.DTO.External.TwelveDataPriceDTO;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Entity.StockInstrument;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwelveDataService {

    @Value("${twelvedata.api.key}")
    private String apiKey;

    @Value("${twelvedata.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;  // ✅ Ekle

    private static final List<String> BIST_SYMBOLS = List.of(
            "THYAO:BIST", "GARAN:BIST", "AKBNK:BIST", "ISCTR:BIST",
            "SAHOL:BIST", "EREGL:BIST", "KCHOL:BIST", "TCELL:BIST",
            "ASELS:BIST", "TUPRS:BIST", "SISE:BIST",  "FROTO:BIST",
            "PETKM:BIST", "KOZAL:BIST", "VAKBN:BIST", "YKBNK:BIST",
            "ENKAI:BIST", "PGSUS:BIST", "BIMAS:BIST", "TTKOM:BIST"
    );

    public InstrumentPrice fetchQuote(String twelveDataSymbol) {
        try {
            String dbSymbol = convertToDbSymbol(twelveDataSymbol);
            String cacheKey = "twelvedata:quote:" + dbSymbol;

            // ✅ Redis'ten oku
            TwelveDataPriceDTO cachedDTO = (TwelveDataPriceDTO) redisTemplate
                    .opsForValue().get(cacheKey);

            if (cachedDTO != null) {
                log.info("✅ Cache HIT - TwelveData: {}", dbSymbol);
                return convertToEntity(cachedDTO);
            }

            log.info("🔍 Cache MISS - Fetching TwelveData: {}", twelveDataSymbol);

            String url = apiUrl + "/price?symbol=" + twelveDataSymbol + "&apikey=" + apiKey;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            String priceStr = root.path("price").asText();
            if (priceStr.isEmpty() || priceStr.equals("null")) {
                log.warn("No price for: {}", twelveDataSymbol);
                return null;
            }

            BigDecimal currentPrice = new BigDecimal(priceStr);

            BaseInstrument instrument = instrumentRepository
                    .findBySymbol(dbSymbol)
                    .orElseGet(() -> createStockInstrument(dbSymbol, twelveDataSymbol));

            // ✅ previousClose DB'den al
            BigDecimal previousClose = priceRepository
                    .findTopByInstrumentOrderByTimestampDesc(instrument)
                    .map(lastPrice -> {
                        boolean isSameDay = lastPrice.getTimestamp()
                                .toLocalDate()
                                .equals(LocalDate.now());
                        return isSameDay
                                ? lastPrice.getPreviousClose()
                                : lastPrice.getCurrentPrice();
                    })
                    .orElse(currentPrice);

            InstrumentPrice price = InstrumentPrice.builder()
                    .instrument(instrument)
                    .currentPrice(currentPrice)
                    .openPrice(currentPrice)
                    .highPrice(currentPrice)
                    .lowPrice(currentPrice)
                    .previousClose(previousClose)
                    .timestamp(LocalDateTime.now())
                    .build();

            priceRepository.save(price);

            // ✅ Redis'e kaydet (8dk TTL)
            TwelveDataPriceDTO dto = TwelveDataPriceDTO.builder()
                    .symbol(dbSymbol)
                    .currentPrice(currentPrice)
                    .previousClose(previousClose)
                    .timestamp(LocalDateTime.now())
                    .build();

            redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofMinutes(8));
            log.info("✅ Cached TwelveData: {} (TTL: 8dk)", dbSymbol);

            log.info("✅ TwelveData: {} = {} (prev: {})",
                    dbSymbol, currentPrice, previousClose);

            return price;

        } catch (Exception e) {
            log.error("❌ TwelveData error for {}: {}", twelveDataSymbol, e.getMessage());
            return null;
        }
    }

    // ✅ DTO'dan Entity'ye
    private InstrumentPrice convertToEntity(TwelveDataPriceDTO dto) {
        BaseInstrument instrument = instrumentRepository
                .findBySymbol(dto.getSymbol())
                .orElseThrow(() -> new RuntimeException(
                        "Instrument not found: " + dto.getSymbol()
                ));

        return InstrumentPrice.builder()
                .instrument(instrument)
                .currentPrice(dto.getCurrentPrice())
                .openPrice(dto.getCurrentPrice())
                .highPrice(dto.getCurrentPrice())
                .lowPrice(dto.getCurrentPrice())
                .previousClose(dto.getPreviousClose())
                .timestamp(dto.getTimestamp())
                .build();
    }

    public int updateBistStocks() {
        log.info("📊 Updating BIST stocks via TwelveData...");
        int updated = 0;

        for (String symbol : BIST_SYMBOLS) {
            try {
                InstrumentPrice price = fetchQuote(symbol);
                if (price != null) {
                    updated++;
                    log.info("Updated {}/{}: {}", updated, BIST_SYMBOLS.size(), symbol);
                }
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("❌ Failed: {}", symbol);
            }
        }

        log.info("✅ BIST updated: {}/{}", updated, BIST_SYMBOLS.size());
        return updated;
    }

    private String convertToDbSymbol(String twelveDataSymbol) {
        if (twelveDataSymbol.endsWith(":BIST")) {
            return twelveDataSymbol.replace(":BIST", ".IS");
        }
        return twelveDataSymbol;
    }

    private BaseInstrument createStockInstrument(String symbol, String twelveDataSymbol) {
        String name = twelveDataSymbol.replace(":BIST", "");
        StockInstrument stock = StockInstrument.builder()
                .symbol(symbol)
                .name(name)
                .sector("Genel")
                .exchange("BIST")
                .currency("TRY")
                .active(true)
                .build();
        instrumentRepository.save(stock);
        log.info("✅ Auto-created BIST Stock: {}", symbol);
        return stock;
    }
}
