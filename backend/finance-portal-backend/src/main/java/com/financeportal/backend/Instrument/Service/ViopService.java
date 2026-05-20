package com.financeportal.backend.Instrument.Service;

import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class ViopService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository instrumentPriceRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final RestTemplate restTemplate;

    @Value("${isyatirim.viop.one-endeks-url}")
    private String oneEndeksUrl;

    @Value("${isyatirim.viop.historical-url}")
    private String historicalUrl;

    private static final List<String> VIOP_SYMBOLS = List.of(
            "F_AKBNK0526", "F_AKBNK0626",
            "F_GARAN0526", "F_GARAN0626",
            "F_THYAO0526", "F_THYAO0626",
            "F_EREGL0526", "F_EREGL0626",
            "F_ISCTR0526", "F_ISCTR0626"
    );

    // ========== INIT ==========

    /**
     * Uygulama başlarken VIOP_SYMBOLS listesindeki kontratları veritabanına kaydeder.
     * Sembol zaten varsa atlar. İsim, vade tarihi ve dayanak varlık sembolden otomatik türetilir.
     */
    @PostConstruct
    public void initViopInstruments() {
        log.info("📋 VİOP enstrümanları başlatılıyor... ({} kontrat)", VIOP_SYMBOLS.size());
        int added = 0;
        for (String symbol : VIOP_SYMBOLS) {
            if (!instrumentRepository.existsBySymbol(symbol)) {
                ViopInstrument viop = ViopInstrument.builder()
                        .symbol(symbol)
                        .name(buildName(symbol))
                        .exchange("VIOP")
                        .currency("TRY")
                        .description(symbol + " vadeli işlem sözleşmesi")
                        .active(true)
                        .underlyingAsset(extractHisse(symbol))
                        .contractType("FUTURES")
                        .expiryDate(extractExpiry(symbol))
                        .build();
                instrumentRepository.save(viop);
                log.info("✅ VİOP enstrüman eklendi: {} → {}", symbol, buildName(symbol));
                added++;
            }
        }
        log.info("✅ VİOP enstrümanları hazır. ({} yeni eklendi, {} zaten vardı)",
                added, VIOP_SYMBOLS.size() - added);
    }

    // ========== SCHEDULED ==========

    /**
     * Her 60 saniyede bir İş Yatırım OneEndeks API'den tüm aktif VİOP kontratlarının
     * anlık fiyatlarını çeker ve instrument_prices tablosuna kaydeder.
     * Ayrıca başlangıç teminatı (initialMargin) bilgisini günceller.
     */
    @Scheduled(fixedDelay = 60000)
    public void fetchViopPrices() {
        log.info("📊 VİOP fiyatları güncelleniyor...");
        List<BaseInstrument> viopInstruments = instrumentRepository.findByExchangeAndActiveTrue("VIOP");
        int updated = 0;

        for (BaseInstrument instrument : viopInstruments) {
            try {
                String url = oneEndeksUrl + instrument.getSymbol();
                List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);

                if (response != null && !response.isEmpty()) {
                    Map<String, Object> data = response.get(0);
                    savePrice(instrument, data);

                    if (instrument instanceof ViopInstrument viop) {
                        BigDecimal margin = toBigDecimal(data.get("initialMargin"));
                        if (margin != null) {
                            viop.setInitialMargin(margin);
                            instrumentRepository.save(viop);
                        }
                    }
                    updated++;
                }
            } catch (Exception e) {
                log.error("❌ VİOP fiyat çekme hatası - {}: {}", instrument.getSymbol(), e.getMessage());
            }
        }
        log.info("✅ VİOP fiyatları güncellendi: {}/{} kontrat", updated, viopInstruments.size());
    }

    /**
     * Her gece 01:00'de vade tarihi geçmiş VİOP kontratlarını pasife çeker.
     * Pasife çekilen kontratlar fiyat güncellemesine dahil edilmez.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void deactivateExpiredContracts() {
        log.info("🔍 Vadesi geçmiş VİOP kontratlar kontrol ediliyor...");
        List<BaseInstrument> viopInstruments = instrumentRepository.findByExchangeAndActiveTrue("VIOP");
        int deactivated = 0;

        for (BaseInstrument i : viopInstruments) {
            if (i instanceof ViopInstrument viop &&
                    viop.getExpiryDate() != null &&
                    viop.getExpiryDate().isBefore(LocalDate.now())) {
                viop.setActive(false);
                instrumentRepository.save(viop);
                log.info("⛔ VİOP kontrat pasife çekildi: {} (vade: {})",
                        viop.getSymbol(), viop.getExpiryDate());
                deactivated++;
            }
        }
        log.info("✅ Vade kontrolü tamamlandı: {} kontrat pasife çekildi", deactivated);
    }

    /**
     * Her gece 02:00'de İş Yatırım IndexHistoricalAll API'den tüm aktif VİOP kontratlarının
     * son 1 yıllık günlük kapanış fiyatlarını çeker ve price_history tablosuna kaydeder.
     * Not: API yalnızca kapanış fiyatı döndürdüğünden open/high/low aynı değere set edilir.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void fetchViopHistoricalData() {
        log.info("📅 VİOP tarihsel veriler çekiliyor...");
        List<BaseInstrument> viopInstruments = instrumentRepository.findByExchangeAndActiveTrue("VIOP");

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        String from = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "000000";
        String to = endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "235959";

        for (BaseInstrument instrument : viopInstruments) {
            try {
                String url = historicalUrl + "?period=1440&from=" + from + "&to=" + to
                        + "&endeks=" + instrument.getSymbol();

                log.info("🔍 VİOP tarihsel veri çekiliyor: {}", instrument.getSymbol());
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                if (response != null && response.containsKey("data")) {
                    List<List<Object>> data = (List<List<Object>>) response.get("data");
                    saveHistoricalData(instrument, data);
                } else {
                    log.warn("⚠️ VİOP tarihsel veri boş döndü: {}", instrument.getSymbol());
                }
            } catch (Exception e) {
                log.error("❌ VİOP tarihsel veri hatası - {}: {}", instrument.getSymbol(), e.getMessage());
            }
        }
        log.info("✅ VİOP tarihsel veriler tamamlandı: {} kontrat işlendi", viopInstruments.size());
    }

    // ========== PRIVATE HELPERS ==========

    /**
     * API'den gelen fiyat verisini instrument_prices tablosuna kaydeder.
     * Mevcut kayıt varsa günceller, yoksa yeni kayıt oluşturur.
     */
    private void savePrice(BaseInstrument instrument, Map<String, Object> data) {
        BigDecimal last     = toBigDecimal(data.get("last"));
        BigDecimal open     = toBigDecimal(data.get("open"));
        BigDecimal high     = toBigDecimal(data.get("high"));
        BigDecimal low      = toBigDecimal(data.get("low"));
        BigDecimal dayClose = toBigDecimal(data.get("dayClose"));
        Long volume         = toLong(data.get("volume"));

        InstrumentPrice price = instrumentPriceRepository
                .findTopByInstrumentOrderByTimestampDesc(instrument)
                .orElse(InstrumentPrice.builder().instrument(instrument).build());

        price.setCurrentPrice(last != null ? last : BigDecimal.ZERO);
        price.setOpenPrice(open != null ? open : BigDecimal.ZERO);
        price.setHighPrice(high != null ? high : BigDecimal.ZERO);
        price.setLowPrice(low != null ? low : BigDecimal.ZERO);
        price.setPreviousClose(dayClose != null ? dayClose : BigDecimal.ZERO);
        price.setVolume(volume);
        price.setTimestamp(LocalDateTime.now());

        instrumentPriceRepository.save(price);
        log.info("✅ VİOP fiyat güncellendi: {} = {} TRY (H:{} L:{})",
                instrument.getSymbol(), last, high, low);
    }

    /**
     * API'den gelen tarihsel veriyi price_history tablosuna kaydeder.
     * Her nokta [timestamp_ms, price] formatındadır.
     * Aynı gün için kayıt varsa günceller, yoksa yeni kayıt oluşturur.
     */
    private void saveHistoricalData(BaseInstrument instrument, List<List<Object>> data) {
        int saved = 0;
        int updated = 0;

        for (List<Object> point : data) {
            try {
                long timestampMs = ((Number) point.get(0)).longValue();
                BigDecimal price = toBigDecimal(point.get(1));

                LocalDate date = java.time.Instant.ofEpochMilli(timestampMs)
                        .atZone(java.time.ZoneId.of("Europe/Istanbul"))
                        .toLocalDate();

                PriceHistory history = priceHistoryRepository
                        .findByInstrumentAndDate(instrument, date)
                        .orElse(PriceHistory.builder().instrument(instrument).date(date).build());

                history.setOpen(price);
                history.setHigh(price);
                history.setLow(price);
                history.setClose(price);
                priceHistoryRepository.save(history);

                if (history.getId() == null) saved++; else updated++;

            } catch (Exception e) {
                log.error("❌ VİOP tarihsel kayıt hatası: {}", e.getMessage());
            }
        }
        log.info("✅ VİOP tarihsel veri kaydedildi: {} → {} yeni, {} güncellendi",
                instrument.getSymbol(), saved, updated);
    }

    /**
     * Sembolden Türkçe kontrat adı türetir.
     */
    private String buildName(String symbol) {
        String hisse = extractHisse(symbol);
        String ayYil = symbol.substring(symbol.length() - 4);
        String ay    = ayYil.substring(0, 2);
        String yil   = "20" + ayYil.substring(2);
        String ayAdi = switch (ay) {
            case "01" -> "Ocak";
            case "02" -> "Şubat";
            case "03" -> "Mart";
            case "04" -> "Nisan";
            case "05" -> "Mayıs";
            case "06" -> "Haziran";
            case "07" -> "Temmuz";
            case "08" -> "Ağustos";
            case "09" -> "Eylül";
            case "10" -> "Ekim";
            case "11" -> "Kasım";
            case "12" -> "Aralık";
            default   -> ay;
        };
        return hisse + " " + ayAdi + " " + yil + " Vadeli";
    }

    /**
     * Sembolden dayanak hisse kodunu çıkarır.
     */
    private String extractHisse(String symbol) {
        String withoutPrefix = symbol.substring(2);
        return withoutPrefix.substring(0, withoutPrefix.length() - 4);
    }

    /**
     * Sembolden vade tarihini çıkarır (ayın son günü).
     */
    private LocalDate extractExpiry(String symbol) {
        String ayYil = symbol.substring(symbol.length() - 4);
        int ay  = Integer.parseInt(ayYil.substring(0, 2));
        int yil = 2000 + Integer.parseInt(ayYil.substring(2));
        int sonGun = LocalDate.of(yil, ay, 1).lengthOfMonth();
        return LocalDate.of(yil, ay, sonGun);
    }

    /**
     * Object tipindeki değeri BigDecimal'e dönüştürür.
     * Integer, Double ve Long tiplerini destekler.
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof Integer i) return BigDecimal.valueOf(i);
        if (value instanceof Double d)  return BigDecimal.valueOf(d);
        if (value instanceof Long l)    return BigDecimal.valueOf(l);
        return new BigDecimal(value.toString());
    }

    /**
     * Object tipindeki değeri Long'a dönüştürür.
     * Integer ve Long tiplerini destekler.
     */
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Long l)    return l;
        return Long.parseLong(value.toString());
    }
}