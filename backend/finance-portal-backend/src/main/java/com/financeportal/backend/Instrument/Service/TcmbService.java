package com.financeportal.backend.Instrument.Service;

import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TcmbService {

    private static final String TCMB_TODAY_URL = "https://www.tcmb.gov.tr/kurlar/today.xml";
    private static final String TCMB_ARCHIVE_URL = "https://www.tcmb.gov.tr/kurlar/%s/%s.xml";

    private final RestTemplate restTemplate;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;

    public List<InstrumentPrice> fetchDailyRates() {
        log.info("Fetching TCMB rates...");

        List<InstrumentPrice> prices = new ArrayList<>();

        try {
            // Bugünkü kurları çek
            String todayXml = restTemplate.getForObject(TCMB_TODAY_URL, String.class);
            if (todayXml == null || todayXml.isEmpty()) {
                log.error("TCMB XML is empty");
                return prices;
            }

            // Dünkü kurları çek (previousClose için)
            Map<String, BigDecimal> yesterdayRates = fetchYesterdayRates();
            log.info("Yesterday rates fetched: {} currencies", yesterdayRates.size());

            // XML parse
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(
                    new ByteArrayInputStream(todayXml.getBytes("UTF-8"))
            );

            NodeList currencies = doc.getElementsByTagName("Currency");

            for (int i = 0; i < currencies.getLength(); i++) {
                Element currency = (Element) currencies.item(i);

                String code = currency.getAttribute("Kod");
                String name = getElementValue(currency, "Isim");
                String forexBuyingStr = getElementValue(currency, "ForexBuying");
                String forexSellingStr = getElementValue(currency, "ForexSelling");

                if (forexBuyingStr == null || forexBuyingStr.isEmpty()) continue;

                String symbol = code + "/TRY";

                BaseInstrument instrument = instrumentRepository
                        .findBySymbol(symbol)
                        .orElseGet(() -> createForexInstrument(symbol, name, code));

                BigDecimal buyPrice  = new BigDecimal(forexBuyingStr.replace(",", "."));
                BigDecimal sellPrice = new BigDecimal(forexSellingStr.replace(",", "."));
                BigDecimal avgPrice  = buyPrice.add(sellPrice)
                        .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);

                // previousClose: önce dünkü arşivden bak, yoksa DB'den al
                BigDecimal previousClose = yesterdayRates.getOrDefault(code, null);

                if (previousClose == null) {
                    // Arşivden gelemediyse DB'deki son farklı günün fiyatına bak
                    previousClose = getPreviousCloseFromDb(instrument, avgPrice);
                }

                InstrumentPrice price = InstrumentPrice.builder()
                        .instrument(instrument)
                        .currentPrice(avgPrice)
                        .openPrice(avgPrice)
                        .highPrice(sellPrice)
                        .lowPrice(buyPrice)
                        .previousClose(previousClose)
                        .timestamp(LocalDateTime.now())
                        .build();

                priceRepository.save(price);
                prices.add(price);

                log.info("Updated TCMB: {} = {} (prev: {}, change: {}%)",
                        symbol, avgPrice, previousClose,
                        calcChangePercent(avgPrice, previousClose));
            }

            log.info("✅ TCMB updated: {} currencies", prices.size());

        } catch (Exception e) {
            log.error("Error fetching TCMB rates: {}", e.getMessage(), e);
        }

        return prices;
    }

    public BigDecimal getExchangeRate(String currency) {
        if (currency == null || currency.equals("TRY")) {
            return BigDecimal.ONE;
        }

        // Kripto stablecoin'leri USD olarak kabul et
        if (currency.equals("USDT") || currency.equals("USDC") || currency.equals("BUSD")) {
            currency = "USD";
        }

        String symbol = currency + "/TRY";

        BigDecimal rate = instrumentRepository.findBySymbol(symbol)
                .flatMap(instrument -> priceRepository
                        .findTopByInstrumentOrderByTimestampDesc(instrument))
                .map(InstrumentPrice::getCurrentPrice)
                .orElse(BigDecimal.ONE);

        log.info("Exchange rate for {}: {} (symbol: {})", currency, rate, symbol);

        return rate;
    }

    public BigDecimal convertFromTRY(BigDecimal amountInTRY, String targetCurrency) {
        if (targetCurrency == null || targetCurrency.equals("TRY")) {
            return amountInTRY;
        }

        BigDecimal exchangeRate = getExchangeRate(targetCurrency);
        if (exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            return amountInTRY;
        }

        return amountInTRY.divide(exchangeRate, 6, RoundingMode.HALF_UP);
    }

    // Dünkü kurları TCMB arşivinden çek
    private Map<String, BigDecimal> fetchYesterdayRates() {
        Map<String, BigDecimal> rates = new HashMap<>();

        try {
            // Dün (hafta sonu varsa Cuma'ya git)
            LocalDate yesterday = getPreviousBusinessDay(LocalDate.now());
            String archiveUrl = buildArchiveUrl(yesterday);

            log.info("Fetching yesterday rates from: {}", archiveUrl);

            String xml = restTemplate.getForObject(archiveUrl, String.class);
            if (xml == null || xml.isEmpty()) {
                log.warn("Yesterday archive empty, trying one more day back");
                // Bir gün daha geri git
                yesterday = getPreviousBusinessDay(yesterday);
                archiveUrl = buildArchiveUrl(yesterday);
                xml = restTemplate.getForObject(archiveUrl, String.class);
            }

            if (xml == null || xml.isEmpty()) {
                log.warn("Could not fetch yesterday rates");
                return rates;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(
                    new ByteArrayInputStream(xml.getBytes("UTF-8"))
            );

            NodeList currencies = doc.getElementsByTagName("Currency");

            for (int i = 0; i < currencies.getLength(); i++) {
                Element currency = (Element) currencies.item(i);
                String code = currency.getAttribute("Kod");
                String forexBuyingStr  = getElementValue(currency, "ForexBuying");
                String forexSellingStr = getElementValue(currency, "ForexSelling");

                if (forexBuyingStr == null || forexBuyingStr.isEmpty()) continue;

                BigDecimal buyPrice  = new BigDecimal(forexBuyingStr.replace(",", "."));
                BigDecimal sellPrice = new BigDecimal(forexSellingStr.replace(",", "."));
                BigDecimal avgPrice  = buyPrice.add(sellPrice)
                        .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);

                rates.put(code, avgPrice);
            }

            log.info("✅ Yesterday rates loaded: {} currencies ({})", rates.size(), yesterday);

        } catch (Exception e) {
            log.warn("Could not fetch yesterday rates: {}", e.getMessage());
        }

        return rates;
    }

    // TCMB arşiv URL'si oluştur
    private String buildArchiveUrl(LocalDate date) {
        // Format: /202602/17022026.xml
        String monthYear = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String dateStr   = date.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        return String.format(TCMB_ARCHIVE_URL, monthYear, dateStr);
    }

    // Hafta sonu kontrolü (Cumartesi/Pazar → Cuma'ya git)
    private LocalDate getPreviousBusinessDay(LocalDate date) {
        LocalDate previous = date.minusDays(1);
        while (previous.getDayOfWeek() == DayOfWeek.SATURDAY ||
                previous.getDayOfWeek() == DayOfWeek.SUNDAY) {
            previous = previous.minusDays(1);
        }
        return previous;
    }

    // Arşiv çekilemediyse DB'den al
    private BigDecimal getPreviousCloseFromDb(BaseInstrument instrument, BigDecimal fallback) {
        return priceRepository
                .findTopByInstrumentOrderByTimestampDesc(instrument)
                .map(lastPrice -> {
                    boolean isSameDay = lastPrice.getTimestamp()
                            .toLocalDate()
                            .equals(LocalDate.now());
                    // Aynı günse o kaydın previousClose'unu kullan
                    return isSameDay
                            ? lastPrice.getPreviousClose()
                            : lastPrice.getCurrentPrice();
                })
                .orElse(fallback);
    }

    // Log için
    private String calcChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return "N/A";
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private BaseInstrument createForexInstrument(String symbol, String name, String code) {
        ForexInstrument forex = ForexInstrument.builder()
                .symbol(symbol)
                .name(name + " / Türk Lirası")
                .baseCurrency(code)
                .quoteCurrency("TRY")
                .exchange("TCMB")
                .currency("TRY")
                .active(true)
                .build();
        instrumentRepository.save(forex);
        log.info("✅ Auto-created ForexInstrument: {}", symbol);
        return forex;
    }

    private String getElementValue(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }
}