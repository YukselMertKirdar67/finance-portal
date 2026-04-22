package com.financeportal.backend.Instrument.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Instrument.DTO.External.YahooPriceDTO;
import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class YahooFinanceService {

    private static final String YAHOO_QUOTE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/%s";
    private static final String YAHOO_HISTORY_URL = "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=%s&range=%s";

    private final RestTemplate restTemplate;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final PriceHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final List<Map<String, String>> US_STOCKS = List.of(
            Map.of("yahoo", "AAPL",  "db", "AAPL",  "name", "Apple Inc.",       "sector", "Teknoloji"),
            Map.of("yahoo", "MSFT",  "db", "MSFT",  "name", "Microsoft Corp.",  "sector", "Teknoloji"),
            Map.of("yahoo", "GOOGL", "db", "GOOGL", "name", "Alphabet Inc.",    "sector", "Teknoloji"),
            Map.of("yahoo", "TSLA",  "db", "TSLA",  "name", "Tesla Inc.",       "sector", "Otomotiv"),
            Map.of("yahoo", "AMZN",  "db", "AMZN",  "name", "Amazon.com Inc.",  "sector", "Teknoloji"),
            Map.of("yahoo", "META",  "db", "META",  "name", "Meta Platforms",   "sector", "Teknoloji"),
            Map.of("yahoo", "NVDA",  "db", "NVDA",  "name", "NVIDIA Corp.",     "sector", "Teknoloji"),
            Map.of("yahoo", "JPM",   "db", "JPM",   "name", "JPMorgan Chase",   "sector", "Finans"),
            Map.of("yahoo", "V",     "db", "V",     "name", "Visa Inc.",        "sector", "Finans"),
            Map.of("yahoo", "WMT",   "db", "WMT",   "name", "Walmart Inc.",     "sector", "Perakende")
    );

    private static final List<Map<String, String>> BIST_STOCKS = List.of(
            Map.of("yahoo", "THYAO.IS", "db", "THYAO.IS", "name", "Türk Hava Yolları",    "sector", "Havacılık"),
            Map.of("yahoo", "GARAN.IS", "db", "GARAN.IS", "name", "Garanti Bankası",      "sector", "Bankacılık"),
            Map.of("yahoo", "AKBNK.IS", "db", "AKBNK.IS", "name", "Akbank",               "sector", "Bankacılık"),
            Map.of("yahoo", "ISCTR.IS", "db", "ISCTR.IS", "name", "İş Bankası",           "sector", "Bankacılık"),
            Map.of("yahoo", "SAHOL.IS", "db", "SAHOL.IS", "name", "Sabancı Holding",      "sector", "Holding"),
            Map.of("yahoo", "EREGL.IS", "db", "EREGL.IS", "name", "Ereğli Demir Çelik",  "sector", "Metal"),
            Map.of("yahoo", "KCHOL.IS", "db", "KCHOL.IS", "name", "Koç Holding",          "sector", "Holding"),
            Map.of("yahoo", "TCELL.IS", "db", "TCELL.IS", "name", "Turkcell",             "sector", "Telekomünikasyon"),
            Map.of("yahoo", "ASELS.IS", "db", "ASELS.IS", "name", "Aselsan",              "sector", "Savunma"),
            Map.of("yahoo", "TUPRS.IS", "db", "TUPRS.IS", "name", "Tüpraş",              "sector", "Enerji"),
            Map.of("yahoo", "SISE.IS",  "db", "SISE.IS",  "name", "Şişe Cam",            "sector", "Cam"),
            Map.of("yahoo", "FROTO.IS", "db", "FROTO.IS", "name", "Ford Otosan",          "sector", "Otomotiv"),
            Map.of("yahoo", "BIMAS.IS", "db", "BIMAS.IS", "name", "BİM Mağazalar",        "sector", "Perakende"),
            Map.of("yahoo", "PGSUS.IS", "db", "PGSUS.IS", "name", "Pegasus",              "sector", "Havacılık"),
            Map.of("yahoo", "YKBNK.IS", "db", "YKBNK.IS", "name", "Yapı Kredi Bankası",  "sector", "Bankacılık")
    );

    private static final List<Map<String, String>> CRYPTOS = List.of(
            Map.of("yahoo", "BTC-USD",  "db", "BTC-USD",  "name", "Bitcoin",       "blockchain", "Bitcoin"),
            Map.of("yahoo", "ETH-USD",  "db", "ETH-USD",  "name", "Ethereum",      "blockchain", "Ethereum"),
            Map.of("yahoo", "BNB-USD",  "db", "BNB-USD",  "name", "BNB",           "blockchain", "BSC"),
            Map.of("yahoo", "XRP-USD",  "db", "XRP-USD",  "name", "XRP",           "blockchain", "XRP Ledger"),
            Map.of("yahoo", "ADA-USD",  "db", "ADA-USD",  "name", "Cardano",       "blockchain", "Cardano"),
            Map.of("yahoo", "SOL-USD",  "db", "SOL-USD",  "name", "Solana",        "blockchain", "Solana"),
            Map.of("yahoo", "DOGE-USD", "db", "DOGE-USD", "name", "Dogecoin",      "blockchain", "Dogecoin"),
            Map.of("yahoo", "DOT-USD",  "db", "DOT-USD",  "name", "Polkadot",      "blockchain", "Polkadot"),
            Map.of("yahoo", "AVAX-USD", "db", "AVAX-USD", "name", "Avalanche",     "blockchain", "Avalanche"),
            Map.of("yahoo", "LINK-USD", "db", "LINK-USD", "name", "Chainlink",     "blockchain", "Ethereum"),
            Map.of("yahoo", "ETC-USD",  "db", "ETC-USD",  "name", "Ethereum Classic", "blockchain", "Ethereum Classic")
    );

    private static final List<Map<String, String>> PRECIOUS_METALS = List.of(
            Map.of("yahoo", "GC=F", "db", "XAU/USD", "name", "Altın (Ons)",    "metalType", "GOLD",      "unit", "oz"),
            Map.of("yahoo", "SI=F", "db", "XAG/USD", "name", "Gümüş (Ons)",   "metalType", "SILVER",    "unit", "oz"),
            Map.of("yahoo", "PL=F", "db", "XPT/USD", "name", "Platin (Ons)",  "metalType", "PLATINUM",  "unit", "oz"),
            Map.of("yahoo", "PA=F", "db", "XPD/USD", "name", "Paladyum (Ons)","metalType", "PALLADIUM", "unit", "oz")
    );
    private static final List<Map<String, String>> FOREX_PAIRS = List.of(
            Map.of("yahoo", "USDTRY=X", "db", "USD/TRY"),
            Map.of("yahoo", "EURTRY=X", "db", "EUR/TRY"),
            Map.of("yahoo", "GBPTRY=X", "db", "GBP/TRY"),
            Map.of("yahoo", "CHFTRY=X", "db", "CHF/TRY"),
            Map.of("yahoo", "JPYTRY=X", "db", "JPY/TRY"),
            Map.of("yahoo", "AUDTRY=X", "db", "AUD/TRY"),
            Map.of("yahoo", "CADTRY=X", "db", "CAD/TRY"),
            Map.of("yahoo", "AEDTRY=X", "db", "AED/TRY")
    );


    public InstrumentPrice fetchQuote(String yahooSymbol, String dbSymbol) {
        String cacheKey = "yahoo:quote:" + dbSymbol;

        // Redis cache kontrolü
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("✅ Cache HIT - Yahoo: {}", dbSymbol);
            return getLatestPriceFromDb(dbSymbol);
        }

        log.info("🔍 Cache MISS - Fetching Yahoo Finance: {}", yahooSymbol);

        try {
            String url = String.format(YAHOO_QUOTE_URL, yahooSymbol);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json,text/plain,*/*");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String response = responseEntity.getBody();

            if (response == null || response.isEmpty()) {
                log.warn("Empty response for: {}", yahooSymbol);
                return null;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("chart").path("result").get(0);

            if (result == null || result.isNull()) {
                log.warn("No result for: {}", yahooSymbol);
                return null;
            }

            JsonNode meta = result.path("meta");

            BigDecimal currentPrice  = new BigDecimal(meta.path("regularMarketPrice").asText("0"));
            BigDecimal previousClose = new BigDecimal(meta.path("chartPreviousClose").asText("0"));
            BigDecimal openPrice     = new BigDecimal(meta.path("regularMarketOpen").asText(currentPrice.toString()));
            BigDecimal highPrice     = new BigDecimal(meta.path("regularMarketDayHigh").asText(currentPrice.toString()));
            BigDecimal lowPrice      = new BigDecimal(meta.path("regularMarketDayLow").asText(currentPrice.toString()));

            if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("Zero price for: {}", yahooSymbol);
                return null;
            }

            BaseInstrument instrument = getOrCreateInstrument(dbSymbol, yahooSymbol, meta);
            Long volume = meta.path("regularMarketVolume").asLong(0);

            BigDecimal changeAmount  = currentPrice.subtract(previousClose);
            BigDecimal changePercent = previousClose.compareTo(BigDecimal.ZERO) > 0
                    ? changeAmount.divide(previousClose, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            InstrumentPrice price = InstrumentPrice.builder()
                    .instrument(instrument)
                    .currentPrice(currentPrice)
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .previousClose(previousClose)
                    .changeAmount(changeAmount)
                    .changePercent(changePercent)
                    .volume(volume)
                    .timestamp(LocalDateTime.now())
                    .build();

            priceRepository.save(price);
            savePriceHistory(instrument, openPrice, highPrice, lowPrice, currentPrice);

            // Redis'e kaydet (5 dakika TTL)

            YahooPriceDTO dto = YahooPriceDTO.builder()
                    .symbol(dbSymbol)
                    .currentPrice(currentPrice)
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .previousClose(previousClose)
                    .changeAmount(changeAmount)
                    .changePercent(changePercent)
                    .timestamp(LocalDateTime.now())
                    .build();

            redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofMinutes(5));

            log.info("✅ Yahoo: {} = {} (prev: {}, change: {}%)",
                    dbSymbol, currentPrice, previousClose,
                    changePercent.setScale(2, java.math.RoundingMode.HALF_UP));

            return price;

        } catch (Exception e) {
            log.error("❌ Yahoo Finance error for {}: {}", yahooSymbol, e.getMessage());
            return null;
        }
    }


    public List<PriceHistory> fetchHistoricalData(String yahooSymbol, String dbSymbol,
                                                  String interval, String range) {
        List<PriceHistory> historyList = new ArrayList<>();

        try {
            String url = String.format(YAHOO_HISTORY_URL, yahooSymbol, interval, range);
            log.info("DEBUG - URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json,text/plain,*/*");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String response = responseEntity.getBody();

            if (response == null) return historyList;

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("chart").path("result").get(0);

            if (result == null || result.isNull()) return historyList;

            JsonNode timestamps = result.path("timestamp");
            JsonNode quotes = result.path("indicators").path("quote").get(0);

            log.info("DEBUG - symbol: {}, timestamps size: {}, quotes null: {}",
                    dbSymbol, timestamps.size(), quotes == null);
            log.info("DEBUG - raw result keys: {}", result.fieldNames());
            log.info("DEBUG - full result: {}", result.toString().substring(0, Math.min(500, result.toString().length())));
            log.info("DEBUG - timestamp node type: {}", timestamps.getNodeType());
            log.info("DEBUG - first timestamp: {}", timestamps.isArray() ? timestamps.get(0) : timestamps);

            if (timestamps == null || quotes == null) return historyList;

            JsonNode opens  = quotes.path("open");
            JsonNode highs  = quotes.path("high");
            JsonNode lows   = quotes.path("low");
            JsonNode closes = quotes.path("close");
            JsonNode volumes = quotes.path("volume");

            BaseInstrument instrument = instrumentRepository.findBySymbol(dbSymbol).orElse(null);
            if (instrument == null) return historyList;

            for (int i = 0; i < timestamps.size(); i++) {
                try {
                    long ts = timestamps.get(i).asLong();
                    LocalDate date = LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC).toLocalDate();

                    if (opens.get(i).isNull() || closes.get(i).isNull()) continue;

                    BigDecimal open  = new BigDecimal(opens.get(i).asText("0"));
                    BigDecimal high  = new BigDecimal(highs.get(i).asText("0"));
                    BigDecimal low   = new BigDecimal(lows.get(i).asText("0"));
                    BigDecimal close = new BigDecimal(closes.get(i).asText("0"));
                    Long vol = volumes.get(i).isNull() ? 0L : volumes.get(i).asLong(0);

                    // Zaten varsa güncelle, yoksa ekle
                    Optional<PriceHistory> existing = historyRepository
                            .findByInstrumentAndDate(instrument, date);

                    if (existing.isPresent()) {
                        PriceHistory h = existing.get();
                        h.setOpen(open); h.setHigh(high);
                        h.setLow(low);   h.setClose(close);
                        historyRepository.save(h);
                    } else {
                        PriceHistory h = PriceHistory.builder()
                                .instrument(instrument)
                                .date(date)
                                .open(open).high(high)
                                .low(low).close(close)
                                .volume(vol)
                                .build();
                        historyRepository.save(h);
                        historyList.add(h);
                    }
                } catch (Exception e) {
                    log.warn("Skipping data point {}: {}", i, e.getMessage());
                }
            }

            log.info("✅ Historical data saved: {} records for {}", historyList.size(), dbSymbol);

        } catch (Exception e) {
            log.error("❌ Historical data error for {}: {}", yahooSymbol, e.getMessage());
        }

        return historyList;
    }


    public int updateUsStocks() {
        log.info("📊 Updating US stocks via Yahoo Finance...");
        int updated = 0;
        for (Map<String, String> stock : US_STOCKS) {
            try {
                InstrumentPrice price = fetchQuote(stock.get("yahoo"), stock.get("db"));
                if (price != null) updated++;
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("❌ Failed: {}", stock.get("db"));
            }
        }
        log.info("✅ US stocks updated: {}/{}", updated, US_STOCKS.size());
        return updated;
    }

    public int updateBistStocks() {
        log.info("📊 Updating BIST stocks via Yahoo Finance...");
        int updated = 0;
        for (Map<String, String> stock : BIST_STOCKS) {
            try {
                InstrumentPrice price = fetchQuote(stock.get("yahoo"), stock.get("db"));
                if (price != null) updated++;
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("❌ Failed: {}", stock.get("db"));
            }
        }
        log.info("✅ BIST stocks updated: {}/{}", updated, BIST_STOCKS.size());
        return updated;
    }

    public int updateCryptos() {
        log.info("📊 Updating cryptos via Yahoo Finance...");
        int updated = 0;
        for (Map<String, String> crypto : CRYPTOS) {
            try {
                InstrumentPrice price = fetchQuote(crypto.get("yahoo"), crypto.get("db"));
                if (price != null) updated++;
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("❌ Failed: {}", crypto.get("db"));
            }
        }
        log.info("✅ Cryptos updated: {}/{}", updated, CRYPTOS.size());
        return updated;
    }

    public int updatePreciousMetals() {
        log.info("📊 Updating precious metals via Yahoo Finance...");
        int updated = 0;
        for (Map<String, String> metal : PRECIOUS_METALS) {
            try {
                InstrumentPrice price = fetchQuote(metal.get("yahoo"), metal.get("db"));
                if (price != null) updated++;
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("❌ Failed: {}", metal.get("db"));
            }
        }
        log.info("✅ Precious metals updated: {}/{}", updated, PRECIOUS_METALS.size());
        return updated;
    }

    public int updateAll() {
        log.info("📊 Updating ALL instruments via Yahoo Finance...");
        int total = 0;
        total += updateUsStocks();
        total += updateBistStocks();
        total += updateCryptos();
        total += updatePreciousMetals();
        log.info("✅ Total updated: {}", total);
        return total;
    }

    public void fetchAllHistoricalData() {
        log.info("📊 Fetching historical data for all instruments...");

        List<Map<String, String>> all = new ArrayList<>();
        all.addAll(US_STOCKS);
        all.addAll(BIST_STOCKS);
        all.addAll(CRYPTOS);
        all.addAll(PRECIOUS_METALS);
        all.addAll(FOREX_PAIRS);

        for (Map<String, String> instrument : all) {
            try {
                fetchHistoricalData(instrument.get("yahoo"), instrument.get("db"), "1d", "1y");
                Thread.sleep(3000);
            } catch (Exception e) {
                log.error("❌ Historical data failed for: {}", instrument.get("db"));
            }
        }

        log.info("✅ Historical data fetch completed");
    }

    // ========== PRIVATE HELPER METHODS ==========

    private BaseInstrument getOrCreateInstrument(String dbSymbol, String yahooSymbol, JsonNode meta) {
        return instrumentRepository.findBySymbol(dbSymbol)
                .orElseGet(() -> createInstrument(dbSymbol, yahooSymbol, meta));
    }

    private BaseInstrument createInstrument(String dbSymbol, String yahooSymbol, JsonNode meta) {
        String currency  = meta.path("currency").asText("USD");
        String exchange  = meta.path("exchangeName").asText("NASDAQ");
        String longName  = meta.path("longName").asText(dbSymbol);

        BaseInstrument instrument;

        // Kıymetli metal kontrolü — önce kontrol et
        if (dbSymbol.startsWith("XAU") || dbSymbol.startsWith("XAG") ||
                dbSymbol.startsWith("XPT") || dbSymbol.startsWith("XPD")) {

            String metalType = switch (dbSymbol.substring(0, 3)) {
                case "XAU" -> "GOLD";
                case "XAG" -> "SILVER";
                case "XPT" -> "PLATINUM";
                case "XPD" -> "PALLADIUM";
                default -> "GOLD";
            };

            instrument = PreciousInstrument.builder()
                    .symbol(dbSymbol).name(longName)
                    .metalType(metalType).unit("oz")
                    .exchange("COMMODITY").currency("USD").active(true).build();

        } else if (dbSymbol.endsWith(".IS")) {
            instrument = StockInstrument.builder()
                    .symbol(dbSymbol).name(longName)
                    .sector("Genel").exchange("BIST")
                    .currency("TRY").active(true).build();

        } else if (dbSymbol.endsWith("-USD")) {
            instrument = CryptoInstrument.builder()
                    .symbol(dbSymbol).name(longName)
                    .blockchain("Unknown").exchange("CRYPTO")
                    .currency("USD").active(true).build();

        } else {
            instrument = StockInstrument.builder()
                    .symbol(dbSymbol).name(longName)
                    .sector("Genel").exchange(exchange)
                    .currency(currency).active(true).build();
        }

        instrumentRepository.save(instrument);
        log.info("✅ Auto-created instrument: {} ({})", dbSymbol, instrument.getInstrumentType());
        return instrument;
    }

    private InstrumentPrice getLatestPriceFromDb(String dbSymbol) {
        return instrumentRepository.findBySymbol(dbSymbol)
                .flatMap(priceRepository::findTopByInstrumentOrderByTimestampDesc)
                .orElse(null);
    }

    private void savePriceHistory(BaseInstrument instrument,
                                  BigDecimal open, BigDecimal high,
                                  BigDecimal low, BigDecimal close) {
        try {
            LocalDate today = LocalDate.now();
            Optional<PriceHistory> existing = historyRepository
                    .findByInstrumentAndDate(instrument, today);

            if (existing.isPresent()) {
                PriceHistory h = existing.get();
                h.setClose(close);
                h.setHigh(high.max(h.getHigh()));
                h.setLow(low.min(h.getLow()));
                historyRepository.save(h);
            } else {
                historyRepository.save(PriceHistory.builder()
                        .instrument(instrument)
                        .date(today)
                        .open(open).high(high)
                        .low(low).close(close)
                        .build());
            }
        } catch (Exception e) {
            log.error("Error saving price history: {}", e.getMessage());
        }
    }
}
