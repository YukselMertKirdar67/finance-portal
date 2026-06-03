package com.financeportal.backend.Instrument;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Repository.*;
import com.financeportal.backend.Instrument.Service.YahooFinanceService;
import com.financeportal.backend.WebSocket.WebSocketPriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("YahooFinanceService Unit Testleri")
class YahooFinanceServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private InstrumentRepository instrumentRepository;
    @Mock private InstrumentPriceRepository priceRepository;
    @Mock private PriceHistoryRepository historyRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private WebSocketPriceService webSocketPriceService;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks
    private YahooFinanceService yahooFinanceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(yahooFinanceService, "yahooQuoteUrl",
                "https://query1.finance.yahoo.com/v8/finance/chart/%s");
        ReflectionTestUtils.setField(yahooFinanceService, "yahooHistoryUrl",
                "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=%s&range=%s");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        doNothing().when(cache).evict(any());
        doNothing().when(webSocketPriceService).sendPriceUpdate(any());
    }

    @Test
    @DisplayName("Cache HIT olduğunda DB'den fiyat dönmeli")
    void fetchQuote_CacheHit_ReturnsFromDb() {
        when(valueOperations.get("yahoo:quote:AAPL")).thenReturn("cached");

        ForexInstrument instrument = ForexInstrument.builder()
                .symbol("AAPL").name("Apple").currency("USD").exchange("NASDAQ").build();
        instrument.setId(1L);

        InstrumentPrice price = InstrumentPrice.builder()
                .instrument(instrument)
                .currentPrice(new BigDecimal("150.00"))
                .build();

        when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(instrument))
                .thenReturn(Optional.of(price));

        InstrumentPrice result = yahooFinanceService.fetchQuote("AAPL", "AAPL");

        assertThat(result).isNotNull();
        assertThat(result.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("API exception durumunda null dönmeli")
    void fetchQuote_ApiException_ReturnsNull() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        InstrumentPrice result = yahooFinanceService.fetchQuote("AAPL", "AAPL");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Boş response için null dönmeli")
    void fetchQuote_EmptyResponse_ReturnsNull() throws Exception {
        when(valueOperations.get(anyString())).thenReturn(null);

        ResponseEntity<String> responseEntity = ResponseEntity.ok("");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(responseEntity);

        InstrumentPrice result = yahooFinanceService.fetchQuote("AAPL", "AAPL");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("updateExistingInstrumentDetails - stock güncellenmeli")
    void updateExistingInstrumentDetails_UpdatesStocks() {
        StockInstrument stock = StockInstrument.builder()
                .symbol("AAPL").name("Apple").currency("USD").exchange("NASDAQ").build();

        when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(stock));
        when(instrumentRepository.save(any())).thenReturn(stock);

        assertThatNoException().isThrownBy(() ->
                yahooFinanceService.updateExistingInstrumentDetails());

        verify(instrumentRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("fetchHistoricalData - enstrüman bulunamadığında boş liste dönmeli")
    void fetchHistoricalData_InstrumentNotFound_ReturnsEmptyList() throws Exception {
        ResponseEntity<String> responseEntity = ResponseEntity.ok(
                "{\"chart\":{\"result\":[{\"timestamp\":[1700000000],\"indicators\":{\"quote\":[{\"open\":[100.0],\"high\":[105.0],\"low\":[99.0],\"close\":[102.0],\"volume\":[1000000]}]}}]}}");

        when(valueOperations.get(anyString())).thenReturn(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(responseEntity);
        when(objectMapper.readTree(anyString())).thenCallRealMethod();
        when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.empty());

        List<PriceHistory> result = yahooFinanceService.fetchHistoricalData(
                "AAPL", "AAPL", "1d", "1y");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchHistoricalData - API exception durumunda boş liste dönmeli")
    void fetchHistoricalData_ApiException_ReturnsEmptyList() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API error"));

        List<PriceHistory> result = yahooFinanceService.fetchHistoricalData(
                "AAPL", "AAPL", "1d", "1y");

        assertThat(result).isEmpty();
    }
}