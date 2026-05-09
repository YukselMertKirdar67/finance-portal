package com.financeportal.backend.Instrument.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.BondInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Entity.PriceHistory;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class TcmbEvdsService {

    @Value("${tcmb.evds.api.key}")
    private String apiKey;

    private static final String EVDS_URL = "https://evds3.tcmb.gov.tr/igmevdsms-dis/";

    private final RestTemplate restTemplate;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final PriceHistoryRepository historyRepository;

    /**
     * TCMB EVDS3'ten faiz oranı verilerini çeker ve veritabanına kaydeder.
     * Bağlantı testi başarısız olursa işlem yapılmaz.
     */
    public List<InstrumentPrice> fetchBondYields() {
        List<InstrumentPrice> prices = new ArrayList<>();

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("❌ TCMB EVDS API key not configured!");
            return prices;
        }

        try {
            // Hafta sonu kontrolü — EVDS hafta sonu veri güncellemiyor
            LocalDate today = LocalDate.now();
            if (today.getDayOfWeek().getValue() >= 6) {
                log.warn("⚠️ Weekend - EVDS does not update on weekends, using last business day");
                // Son iş gününe geri git
                while (today.getDayOfWeek().getValue() >= 6) {
                    today = today.minusDays(1);
                }
            }

            String dateStr = today.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            log.info("📊 Fetching bond yields from TCMB EVDS3");
            log.info("📅 Date: {}", dateStr);

            if (!testConnection(dateStr)) {
                log.error("❌ EVDS connection test failed!");
                return prices;
            }

            prices.addAll(fetchBondYield("TP.APIFON4",           "TR-AOFM",    "TCMB Ort. Fonlama Maliyeti",  dateStr));
            prices.addAll(fetchBondYield("TP.BISTTLREF.KAPANIS", "TR-TLREF",   "TL Gecelik Referans Faizi",   dateStr));
            prices.addAll(fetchBondYield("TP.BISTTLREF.YUKSEK",  "TR-TLREF-H", "TL Referans Faizi Yüksek",    dateStr));
            prices.addAll(fetchBondYield("TP.BISTTLREF.DUSUK",   "TR-TLREF-D", "TL Referans Faizi Düşük",     dateStr));

            log.info("✅ Total bond yields updated: {}", prices.size());

        } catch (Exception e) {
            log.error("❌ Error fetching bond yields: {}", e.getMessage(), e);
        }

        return prices;
    }

    /**
     * TCMB EVDS3'ten belirtilen tarih aralığındaki faiz oranı geçmiş verilerini çeker.
     * Hafta sonları ve null değerler atlanır.
     */
    public void fetchBondYieldsHistorical(LocalDate startDate, LocalDate endDate) {
        log.info("📊 Fetching historical bond yields from {} to {}", startDate, endDate);

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("❌ TCMB EVDS API key not configured!");
            return;
        }

        String startDateStr = startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String endDateStr   = endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        fetchBondYieldHistorical("TP.APIFON4",           "TR-AOFM",    startDateStr, endDateStr);
        fetchBondYieldHistorical("TP.BISTTLREF.KAPANIS", "TR-TLREF",   startDateStr, endDateStr);
        fetchBondYieldHistorical("TP.BISTTLREF.YUKSEK",  "TR-TLREF-H", startDateStr, endDateStr);
        fetchBondYieldHistorical("TP.BISTTLREF.DUSUK",   "TR-TLREF-D", startDateStr, endDateStr);

        log.info("✅ Historical bond yields fetch completed");
    }

    /**
     * EVDS3 API bağlantısını test eder.
     * USD/TRY döviz kuru çekerek bağlantının çalıştığını doğrular.
     */
    private boolean testConnection(String date) {
        try {
            String url = EVDS_URL +
                    "series=TP.DK.USD.A.YTL" +
                    "&startDate=" + date +
                    "&endDate=" + date +
                    "&type=json";

            log.info("🔍 Testing EVDS3 connection...");

            HttpHeaders headers = new HttpHeaders();
            headers.set("key", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null &&
                    !response.getBody().isEmpty() &&
                    !response.getBody().startsWith("<")) {
                log.info("✅ EVDS3 connection successful!");
                return true;
            }


            log.error("❌ EVDS3 returned unexpected response");
            log.error("❌ Status: {}", response.getStatusCode());
            log.error("❌ Body (first 300 chars): {}",
                    response.getBody() != null ? response.getBody().substring(0, Math.min(300, response.getBody().length())) : "null");
            return false;

        } catch (Exception e) {
            log.error("❌ EVDS3 connection failed: {}", e.getMessage());

            if (e.getMessage() != null) {
                if (e.getMessage().contains("403")) {
                    log.error("🔑 403 Forbidden - API key is invalid or not activated");
                } else if (e.getMessage().contains("401")) {
                    log.error("🔑 401 Unauthorized - API key is missing or wrong");
                }
            }

            return false;
        }
    }

    /**
     * Belirtilen EVDS seri kodundan faiz verisi çeker ve kaydeder.
     * Veri boş veya null gelirse kayıt yapılmaz.
     */
    private List<InstrumentPrice> fetchBondYield(
            String seriesCode,
            String symbol,
            String name,
            String date) {

        List<InstrumentPrice> prices = new ArrayList<>();

        try {
            String url = EVDS_URL +
                    "series=" + seriesCode +
                    "&startDate=" + date +
                    "&endDate=" + date +
                    "&type=json";

            log.debug("📊 Fetching: {} ({})", symbol, seriesCode);

            HttpHeaders headers = new HttpHeaders();
            headers.set("key", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            String responseBody = response.getBody();

            if (responseBody == null || responseBody.isEmpty() || responseBody.startsWith("<")) {
                log.warn("⚠️ Invalid response for: {} - got HTML instead of JSON", symbol);
                return prices;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            JsonNode items = root.path("items");

            if (items.isEmpty() || items.isNull()) {
                log.warn("⚠️ No items for: {}", symbol);
                return prices;
            }

            JsonNode data = items.get(0);

            String fieldName = seriesCode.replace(".", "_");
            String yieldStr = data.path(fieldName).asText();

            if (yieldStr == null || yieldStr.isEmpty() ||
                    yieldStr.equals("null") || yieldStr.equals("ND")) {
                log.warn("⚠️ No yield data for: {} (field: {})", symbol, fieldName);
                return prices;
            }

            BigDecimal yieldRate = new BigDecimal(yieldStr.replace(",", "."));

            BaseInstrument instrument = instrumentRepository
                    .findBySymbol(symbol)
                    .orElseGet(() -> createBondInstrument(symbol, name));

            InstrumentPrice previousPrice = priceRepository
                    .findTopByInstrumentOrderByTimestampDesc(instrument)
                    .orElse(null);

            BigDecimal previousClose = previousPrice != null
                    ? previousPrice.getCurrentPrice()
                    : yieldRate;

            InstrumentPrice price = InstrumentPrice.builder()
                    .instrument(instrument)
                    .currentPrice(yieldRate)
                    .openPrice(yieldRate)
                    .highPrice(yieldRate)
                    .lowPrice(yieldRate)
                    .previousClose(previousClose)
                    .yieldRate(yieldRate)
                    .timestamp(LocalDateTime.now())
                    .build();

            priceRepository.save(price);
            savePriceHistory(instrument, yieldRate, yieldRate, yieldRate, yieldRate);
            prices.add(price);

            log.info("✅ Updated: {} = {}%", symbol, yieldRate);

        } catch (Exception e) {
            log.error("❌ Error for {}: {}", symbol, e.getMessage());
        }

        return prices;
    }

    /**
     * Günlük fiyat geçmişini kaydeder.
     * Aynı gün için kayıt varsa günceller, yoksa yeni kayıt oluşturur.
     */
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
            } else {
                PriceHistory history = PriceHistory.builder()
                        .instrument(instrument)
                        .date(today)
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .yieldRate(close)
                        .build();
                historyRepository.save(history);
                log.info("✅ Saved history for: {} on {}", instrument.getSymbol(), today);
            }
        } catch (Exception e) {
            log.error("❌ Error saving price history: {}", e.getMessage());
        }
    }

    /**
     * Belirtilen EVDS seri kodundan geçmiş faiz verisi çeker ve PriceHistory'e kaydeder.
     */
    private void fetchBondYieldHistorical(String seriesCode, String symbol,
                                          String startDate, String endDate) {
        try {
            String url = EVDS_URL +
                    "series=" + seriesCode +
                    "&startDate=" + startDate +
                    "&endDate=" + endDate +
                    "&type=json";

            HttpHeaders headers = new HttpHeaders();
            headers.set("key", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.startsWith("<")) {
                log.warn("⚠️ Invalid response for: {}", symbol);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            JsonNode items = root.path("items");

            if (items.isEmpty()) {
                log.warn("⚠️ No items for: {}", symbol);
                return;
            }

            BaseInstrument instrument = instrumentRepository.findBySymbol(symbol).orElse(null);
            if (instrument == null) {
                log.warn("⚠️ Instrument not found: {}", symbol);
                return;
            }

            String fieldName = seriesCode.replace(".", "_");
            int saved = 0;

            for (JsonNode item : items) {
                try {
                    String dateStr  = item.path("Tarih").asText();
                    String yieldStr = item.path(fieldName).asText();

                    log.info("DEBUG - date: {}, yield: {}, field: {}", dateStr, yieldStr, fieldName);


                    if (yieldStr == null || yieldStr.isEmpty() ||
                            yieldStr.equals("null") || yieldStr.equals("ND")) continue;

                    LocalDate date = LocalDate.parse(dateStr,
                            DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    BigDecimal yieldRate = new BigDecimal(yieldStr.replace(",", "."));

                    Optional<PriceHistory> existing = historyRepository
                            .findByInstrumentAndDate(instrument, date);

                    if (existing.isPresent()) {
                        PriceHistory h = existing.get();
                        h.setClose(yieldRate);
                        h.setYieldRate(yieldRate);
                        historyRepository.save(h);
                    } else {
                        historyRepository.save(PriceHistory.builder()
                                .instrument(instrument)
                                .date(date)
                                .open(yieldRate).high(yieldRate)
                                .low(yieldRate).close(yieldRate)
                                .yieldRate(yieldRate)
                                .build());
                        saved++;
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Skipping data point: {}", e.getMessage());
                }
            }

            log.info("✅ Historical saved: {} records for {}", saved, symbol);

        } catch (Exception e) {
            log.error("❌ Error fetching historical for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Sistemde olmayan tahvil enstrümanını otomatik oluşturur.
     */
    private BaseInstrument createBondInstrument(String symbol, String name) {
        BondInstrument bond = BondInstrument.builder()
                .symbol(symbol)
                .name(name)
                .issuer("Hazine ve Maliye Bakanlığı")
                .exchange("TCMB")
                .currency("TRY")
                .active(true)
                .build();

        instrumentRepository.save(bond);
        log.info("✅ Auto-created Bond: {}", symbol);
        return bond;
    }
}