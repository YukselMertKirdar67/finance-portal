package com.financeportal.backend.Instrument;

import com.financeportal.backend.Instrument.Entity.ForexInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Repository.PriceHistoryRepository;
import com.financeportal.backend.Instrument.Service.TcmbService;
import com.financeportal.backend.WebSocket.WebSocketPriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TcmbService Unit Tests")
class TcmbServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentPriceRepository priceRepository;

    @Mock
    private PriceHistoryRepository historyRepository;

    @Mock
    private WebSocketPriceService webSocketPriceService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private TcmbService tcmbService;

    private ForexInstrument testInstrument;
    private InstrumentPrice testPrice;

    @BeforeEach
    void setUp() {
        testInstrument = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları / Türk Lirası")
                .baseCurrency("USD")
                .quoteCurrency("TRY")
                .exchange("TCMB")
                .currency("TRY")
                .build();
        testInstrument.setActive(true);

        testPrice = InstrumentPrice.builder()
                .instrument(testInstrument)
                .currentPrice(new BigDecimal("32.50"))
                .openPrice(new BigDecimal("32.00"))
                .highPrice(new BigDecimal("33.00"))
                .lowPrice(new BigDecimal("32.00"))
                .previousClose(new BigDecimal("32.00"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ========== GET EXCHANGE RATE TESTS ==========

    @Test
    @DisplayName("TRY için kur oranı 1 dönmeli")
    void getExchangeRate_TRY_ReturnsOne() {
        BigDecimal rate = tcmbService.getExchangeRate("TRY");
        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Null currency için kur oranı 1 dönmeli")
    void getExchangeRate_Null_ReturnsOne() {
        BigDecimal rate = tcmbService.getExchangeRate(null);
        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("USD için kur oranı DB'den getirilmeli")
    void getExchangeRate_USD_ReturnsFromDb() {
        when(instrumentRepository.findBySymbol("USD/TRY"))
                .thenReturn(Optional.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(testPrice));

        BigDecimal rate = tcmbService.getExchangeRate("USD");

        assertThat(rate).isEqualByComparingTo(new BigDecimal("32.50"));
        verify(instrumentRepository, times(1)).findBySymbol("USD/TRY");
    }

    @Test
    @DisplayName("DB'de kur yoksa 1 dönmeli")
    void getExchangeRate_NotInDb_ReturnsOne() {
        when(instrumentRepository.findBySymbol("EUR/TRY"))
                .thenReturn(Optional.empty());

        BigDecimal rate = tcmbService.getExchangeRate("EUR");

        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("USDT stablecoin için USD kuru kullanılmalı")
    void getExchangeRate_USDT_UsesUSDRate() {
        when(instrumentRepository.findBySymbol("USD/TRY"))
                .thenReturn(Optional.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(testPrice));

        BigDecimal rate = tcmbService.getExchangeRate("USDT");

        assertThat(rate).isEqualByComparingTo(new BigDecimal("32.50"));
        verify(instrumentRepository, times(1)).findBySymbol("USD/TRY");
    }

    // ========== CONVERT FROM TRY TESTS ==========

    @Test
    @DisplayName("TRY'den TRY'ye çeviri aynı tutarı dönmeli")
    void convertFromTRY_ToTRY_ReturnsSameAmount() {
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal result = tcmbService.convertFromTRY(amount, "TRY");
        assertThat(result).isEqualByComparingTo(amount);
    }

    @Test
    @DisplayName("TRY'den USD'ye çeviri doğru hesaplanmalı")
    void convertFromTRY_ToUSD_CorrectConversion() {
        when(instrumentRepository.findBySymbol("USD/TRY"))
                .thenReturn(Optional.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(testPrice));

        BigDecimal amountInTRY = new BigDecimal("325.00");
        BigDecimal result = tcmbService.convertFromTRY(amountInTRY, "USD");

        // 325 TRY / 32.50 USD/TRY = 10 USD
        assertThat(result).isEqualByComparingTo(new BigDecimal("10.000000"));
    }

    @Test
    @DisplayName("Null currency için TRY tutarı değişmeden dönmeli")
    void convertFromTRY_NullCurrency_ReturnsSameAmount() {
        BigDecimal amount = new BigDecimal("500.00");
        BigDecimal result = tcmbService.convertFromTRY(amount, null);
        assertThat(result).isEqualByComparingTo(amount);
    }
}
