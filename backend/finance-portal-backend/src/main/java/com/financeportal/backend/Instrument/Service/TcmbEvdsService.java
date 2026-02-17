/*package com.financeportal.backend.Instrument.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.BondInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TcmbEvdsService {

    @Value("${tcmb.evds.api.key}")
    private String apiKey;

    // ✅ EVDS3 - URL hala evds2 (backend EVDS3)
    private static final String EVDS_URL = "https://evds2.tcmb.gov.tr/service/evds/";

    private final RestTemplate restTemplate;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;

    public List<InstrumentPrice> fetchBondYields() {
        List<InstrumentPrice> prices = new ArrayList<>();

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("❌ TCMB EVDS API key not configured!");
            log.error("Get your key from: https://evds2.tcmb.gov.tr/");
            return prices;
        }

        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            log.info("📊 Fetching bond yields from TCMB EVDS3");
            log.info("📅 Date: {}", today);
            log.info("🔑 API Key: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));

            // ✅ Test connection first
            if (!testConnection(today)) {
                log.error("❌ EVDS connection test failed!");
                return prices;
            }

            // ✅ Fetch bond yields
            prices.addAll(fetchBondYield("TP.GOV.SEC.YIELD.3M", "TR-3M-BOND", "3 Aylık Hazine Bonosu", today));
            prices.addAll(fetchBondYield("TP.GOV.SEC.YIELD.6M", "TR-6M-BOND", "6 Aylık Hazine Bonosu", today));
            prices.addAll(fetchBondYield("TP.GOV.SEC.YIELD.1Y", "TR-1Y-BOND", "1 Yıllık Devlet Tahvili", today));
            prices.addAll(fetchBondYield("TP.GOV.SEC.YIELD.2Y", "TR-2Y-BOND", "2 Yıllık Devlet Tahvili", today));
            prices.addAll(fetchBondYield("TP.GOV.SEC.YIELD.5Y", "TR-5Y-BOND", "5 Yıllık Devlet Tahvili", today));
            prices.addAll(fetchBondYield("TP.GOV.SEC.YIELD.10Y", "TR-10Y-BOND", "10 Yıllık Devlet Tahvili", today));

            log.info("✅ Total bond yields updated: {}", prices.size());

        } catch (Exception e) {
            log.error("❌ Error fetching bond yields: {}", e.getMessage(), e);
        }

        return prices;
    }

    private boolean testConnection(String date) {
        try {
            // ✅ Test with USD/TRY (always available)
            String url = EVDS_URL +
                    "series=TP.DK.USD.A" +
                    "&startDate=" + date +
                    "&endDate=" + date +
                    "&type=json" +
                    "&key=" + apiKey;

            log.info("🔍 Testing EVDS3 connection...");
            log.debug("URL: {}", url.replace(apiKey, "***"));

            String response = restTemplate.getForObject(url, String.class);

            if (response != null && !response.isEmpty()) {
                log.info("✅ EVDS3 connection successful!");
                log.debug("Response: {}", response.substring(0, Math.min(100, response.length())));
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("❌ EVDS3 connection failed: {}", e.getMessage());

            if (e.getMessage().contains("403")) {
                log.error("🔑 403 Forbidden - API key is invalid or not activated");
                log.error("   1. Check your key at: https://evds2.tcmb.gov.tr/");
                log.error("   2. Make sure key is activated (wait 5-10 minutes after creation)");
                log.error("   3. Try generating a new key");
            } else if (e.getMessage().contains("401")) {
                log.error("🔑 401 Unauthorized - API key is missing or wrong");
            }

            return false;
        }
    }

    private List<InstrumentPrice> fetchBondYield(
            String seriesCode,
            String symbol,
            String name,
            String date
    ) {
        List<InstrumentPrice> prices = new ArrayList<>();

        try {
            String url = EVDS_URL +
                    "series=" + seriesCode +
                    "&startDate=" + date +
                    "&endDate=" + date +
                    "&type=json" +
                    "&key=" + apiKey;

            log.debug("📊 Fetching: {}", symbol);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isEmpty()) {
                log.warn("Empty response for: {}", symbol);
                return prices;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            // ✅ EVDS response: { "items": [ { "Tarih": "...", "TP_GOV_SEC_YIELD_10Y": "..." } ] }
            JsonNode items = root.path("items");

            if (items.isEmpty() || items.isNull()) {
                log.warn("No items for: {}", symbol);
                return prices;
            }

            JsonNode data = items.get(0);

            // ✅ Series code: TP.GOV.SEC.YIELD.10Y → Field name: TP_GOV_SEC_YIELD_10Y
            String fieldName = seriesCode.replace(".", "_");
            String yieldStr = data.path(fieldName).asText();

            if (yieldStr == null || yieldStr.isEmpty() || yieldStr.equals("null")) {
                log.warn("No yield data for: {} (field: {})", symbol, fieldName);
                return prices;
            }

            // ✅ Turkish decimal: 14,35 → 14.35
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
            prices.add(price);

            log.info("✅ Updated: {} = {}%", symbol, yieldRate);

        } catch (Exception e) {
            log.error("❌ Error for {}: {}", symbol, e.getMessage());
        }

        return prices;
    }

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
}*/
