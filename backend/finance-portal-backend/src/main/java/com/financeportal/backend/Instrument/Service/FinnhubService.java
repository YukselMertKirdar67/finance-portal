package com.financeportal.backend.Instrument.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.financeportal.backend.Instrument.DTO.External.FinnhubPriceDTO;
import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Instrument.Repository.*;

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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinnhubService {

    @Value("${finance.api.key}")
    private String apiKey;

    private static final String FINNHUB_QUOTE_URL = "https://finnhub.io/api/v1/quote";

    private final RestTemplate restTemplate;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final PriceHistoryRepository historyRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public InstrumentPrice fetchQuote(String symbol) {
        String cacheKey = "finnhub:quote:" + symbol;
        FinnhubPriceDTO cachedDTO = (FinnhubPriceDTO) redisTemplate.opsForValue().get(cacheKey);

        if (cachedDTO != null) {
            log.info("✅ Cache HIT - Using cached price for: {}", symbol);
            return convertToEntity(cachedDTO);
        }

        log.info("🔍 Cache MISS - Fetching from Finnhub API: {}", symbol);

        try {
            String url = FINNHUB_QUOTE_URL + "?symbol=" + symbol + "&token=" + apiKey;
            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            BigDecimal currentPrice  = new BigDecimal(root.path("c").asText());
            BigDecimal previousClose = new BigDecimal(root.path("pc").asText());
            BigDecimal highPrice     = new BigDecimal(root.path("h").asText());
            BigDecimal lowPrice      = new BigDecimal(root.path("l").asText());
            BigDecimal openPrice     = new BigDecimal(root.path("o").asText());

            if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("No valid price data for symbol: {}", symbol);
                return null;
            }

            BaseInstrument instrument = instrumentRepository
                    .findBySymbol(symbol)
                    .orElseGet(() -> createInstrumentFromSymbol(symbol));

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

            // Tarihsel kayıt ekle
            savePriceHistory(instrument, openPrice, highPrice, lowPrice, currentPrice);

            // Redis cache
            FinnhubPriceDTO dto = FinnhubPriceDTO.builder()
                    .symbol(symbol)
                    .currentPrice(currentPrice)
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .previousClose(previousClose)
                    .timestamp(LocalDateTime.now())
                    .build();

            redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofSeconds(60));
            log.info("✅ Cached price for: {} (TTL: 60s)", symbol);
            log.info("Updated Finnhub: {} = {}", symbol, currentPrice);

            return price;

        } catch (Exception e) {
            log.error("Error fetching Finnhub quote for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    // PriceHistory kaydet
    private void savePriceHistory(BaseInstrument instrument,
                                  BigDecimal open, BigDecimal high,
                                  BigDecimal low, BigDecimal close) {
        try {
            LocalDate today = LocalDate.now();

            Optional<PriceHistory> existing = historyRepository
                    .findByInstrumentAndDate(instrument, today);

            if (existing.isPresent()) {
                PriceHistory history = existing.get();
                history.setClose(close);
                history.setHigh(high.max(history.getHigh()));
                history.setLow(low.min(history.getLow()));
                historyRepository.save(history);
                log.debug("Updated history for: {} on {}", instrument.getSymbol(), today);
            } else {
                PriceHistory history = PriceHistory.builder()
                        .instrument(instrument)
                        .date(today)
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .build();
                historyRepository.save(history);
                log.info("✅ Saved history for: {} on {}", instrument.getSymbol(), today);
            }
        } catch (Exception e) {
            log.error("Error saving price history: {}", e.getMessage());
        }
    }
    private InstrumentPrice convertToEntity(FinnhubPriceDTO dto) {
        BaseInstrument instrument = instrumentRepository
                .findBySymbol(dto.getSymbol())
                .orElseThrow(() -> new RuntimeException("Instrument not found: " + dto.getSymbol()));

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

    private BaseInstrument createInstrumentFromSymbol(String symbol) {
        if (symbol.startsWith("BINANCE:")) {
            CryptoInstrument crypto = CryptoInstrument.builder()
                    .symbol(symbol)
                    .name(symbol.replace("BINANCE:", "").replace("USDT", ""))
                    .blockchain("Binance Smart Chain")
                    .exchange("BINANCE")
                    .currency("USDT")
                    .active(true)
                    .build();
            instrumentRepository.save(crypto);
            log.info("✅ Auto-created CryptoInstrument: {}", symbol);
            return crypto;
        } else if (symbol.endsWith(".IS")) {
            StockInstrument stock = StockInstrument.builder()
                    .symbol(symbol)
                    .name(symbol.replace(".IS", ""))
                    .sector("Genel")
                    .exchange("BIST")
                    .currency("TRY")
                    .active(true)
                    .build();
            instrumentRepository.save(stock);
            log.info("✅ Auto-created StockInstrument (BIST): {}", symbol);
            return stock;
        } else {
            StockInstrument stock = StockInstrument.builder()
                    .symbol(symbol)
                    .name(symbol)
                    .sector("Teknoloji")
                    .exchange("NASDAQ")
                    .currency("USD")
                    .active(true)
                    .build();
            instrumentRepository.save(stock);
            log.info("✅ Auto-created StockInstrument (US): {}", symbol);
            return stock;
        }
    }

    public int updatePriorityInstruments() {
        List<String> prioritySymbols = List.of(
                "AAPL", "MSFT", "GOOGL", "TSLA",
                "BINANCE:BTCUSDT", "BINANCE:ETHUSDT"
        );

        int updated = 0;
        for (String symbol : prioritySymbols) {
            try {
                InstrumentPrice price = fetchQuote(symbol);
                if (price != null) {
                    updated++;
                    log.info("Updated {}/{}: {}", updated, prioritySymbols.size(), symbol);
                }
                Thread.sleep(1100);
            } catch (Exception e) {
                log.error("Failed to update: {}", symbol);
            }
        }

        log.info("✅ Total updated: {} instruments", updated);
        return updated;
    }

    public int updateCryptos() {
        List<String> cryptoSymbols = List.of(
                "BINANCE:BTCUSDT", "BINANCE:ETHUSDT", "BINANCE:BNBUSDT",
                "BINANCE:XRPUSDT", "BINANCE:ADAUSDT", "BINANCE:SOLUSDT"
        );

        int updated = 0;
        for (String symbol : cryptoSymbols) {
            try {
                InstrumentPrice price = fetchQuote(symbol);
                if (price != null) updated++;
                Thread.sleep(1100);
            } catch (Exception e) {
                log.error("Failed to update crypto: {}", symbol);
            }
        }

        log.info("✅ Crypto updated: {}", updated);
        return updated;
    }

    public int updateUsStocks() {
        List<String> usSymbols = List.of(
                "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
                "META", "NVDA", "JPM", "V", "WMT"
        );

        int updated = 0;
        for (String symbol : usSymbols) {
            try {
                InstrumentPrice price = fetchQuote(symbol);
                if (price != null) updated++;
                Thread.sleep(1100);
            } catch (Exception e) {
                log.error("Failed to update US stock: {}", symbol);
            }
        }

        log.info("✅ US stocks updated: {}", updated);
        return updated;
    }

    public int updateAllInstruments() {
        List<BaseInstrument> instruments = instrumentRepository.findByActiveTrue();

        List<BaseInstrument> finnhubInstruments = instruments.stream()
                .filter(i -> i.getInstrumentType() == InstrumentType.STOCK ||
                        i.getInstrumentType() == InstrumentType.CRYPTO)
                .toList();

        int updated = 0;
        for (BaseInstrument instrument : finnhubInstruments) {
            try {
                InstrumentPrice price = fetchQuote(instrument.getSymbol());
                if (price != null) {
                    updated++;
                    log.info("Updated {}/{}: {}", updated, finnhubInstruments.size(), instrument.getSymbol());
                }
                Thread.sleep(1100);
            } catch (Exception e) {
                log.error("Failed to update: {}", instrument.getSymbol());
            }
        }

        log.info("✅ Total updated: {} instruments", updated);
        return updated;
    }
}
