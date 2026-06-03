package com.financeportal.backend.Home;

import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.DTO.PageResponseDTO;
import com.financeportal.backend.News.Service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Home Service Unit Tests")
class HomeServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentPriceRepository priceRepository;

    @Mock
    private NewsService newsService;

    @InjectMocks
    private HomeService homeService;

    private ForexInstrument testInstrument;
    private InstrumentPrice positivePrice;
    private InstrumentPrice negativePrice;

    @BeforeEach
    void setUp() {
        testInstrument = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .currency("TRY")
                .exchange("TCMB")
                .build();
        testInstrument.setActive(true);

        positivePrice = InstrumentPrice.builder()
                .instrument(testInstrument)
                .currentPrice(new BigDecimal("38.50"))
                .changeAmount(new BigDecimal("0.50"))
                .changePercent(new BigDecimal("1.32"))
                .previousClose(new BigDecimal("38.00"))
                .timestamp(LocalDateTime.now())
                .build();

        negativePrice = InstrumentPrice.builder()
                .instrument(testInstrument)
                .currentPrice(new BigDecimal("37.50"))
                .changeAmount(new BigDecimal("-0.50"))
                .changePercent(new BigDecimal("-1.32"))
                .previousClose(new BigDecimal("38.00"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Anasayfa verisi başarıyla oluşturulmalı")
    void getHomePageData_Success() {
        when(instrumentRepository.findBySymbol(anyString())).thenReturn(Optional.empty());
        when(instrumentRepository.findByActiveTrue()).thenReturn(List.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(positivePrice));
        when(instrumentRepository.countByType(any())).thenReturn(5L);
        when(newsService.getAllNews(eq(0), eq(5))).thenReturn(
                new PageResponseDTO<>(List.of(), 0, 5, 0, 0, true));

        HomePageDTO result = homeService.getHomePageData();

        assertThat(result).isNotNull();
        assertThat(result.getMarketStats()).isNotNull();
        assertThat(result.getCategories()).isNotNull();
    }

    @Test
    @DisplayName("Piyasa istatistikleri doğru hesaplanmalı - yükselen")
    void getHomePageData_MarketStats_Rising() {
        when(instrumentRepository.findBySymbol(anyString())).thenReturn(Optional.empty());
        when(instrumentRepository.findByActiveTrue()).thenReturn(List.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(positivePrice));
        when(instrumentRepository.countByType(any())).thenReturn(0L);
        when(newsService.getAllNews(anyInt(), anyInt())).thenReturn(
                new PageResponseDTO<>(List.of(), 0, 5, 0, 0, true));

        HomePageDTO result = homeService.getHomePageData();

        assertThat(result.getMarketStats().getRising()).isEqualTo(1);
        assertThat(result.getMarketStats().getFalling()).isEqualTo(0);
    }

    @Test
    @DisplayName("Piyasa istatistikleri doğru hesaplanmalı - düşen")
    void getHomePageData_MarketStats_Falling() {
        when(instrumentRepository.findBySymbol(anyString())).thenReturn(Optional.empty());
        when(instrumentRepository.findByActiveTrue()).thenReturn(List.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(negativePrice));
        when(instrumentRepository.countByType(any())).thenReturn(0L);
        when(newsService.getAllNews(anyInt(), anyInt())).thenReturn(
                new PageResponseDTO<>(List.of(), 0, 5, 0, 0, true));

        HomePageDTO result = homeService.getHomePageData();

        assertThat(result.getMarketStats().getFalling()).isEqualTo(1);
        assertThat(result.getMarketStats().getRising()).isEqualTo(0);
    }

    @Test
    @DisplayName("Haber servisi hata verirse boş liste dönmeli")
    void getHomePageData_NewsServiceFails_ReturnsEmptyList() {
        when(instrumentRepository.findBySymbol(anyString())).thenReturn(Optional.empty());
        when(instrumentRepository.findByActiveTrue()).thenReturn(List.of());
        when(instrumentRepository.countByType(any())).thenReturn(0L);
        when(newsService.getAllNews(anyInt(), anyInt())).thenThrow(new RuntimeException("News service error"));

        HomePageDTO result = homeService.getHomePageData();

        assertThat(result).isNotNull();
        assertThat(result.getRecentNews()).isEmpty();
    }

    @Test
    @DisplayName("Enstrüman fiyatı yoksa top gainers'a dahil edilmemeli")
    void getHomePageData_NoPriceData_NotInGainers() {
        when(instrumentRepository.findBySymbol(anyString())).thenReturn(Optional.empty());
        when(instrumentRepository.findByActiveTrue()).thenReturn(List.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.empty());
        when(instrumentRepository.countByType(any())).thenReturn(0L);
        when(newsService.getAllNews(anyInt(), anyInt())).thenReturn(
                new PageResponseDTO<>(List.of(), 0, 5, 0, 0, true));

        HomePageDTO result = homeService.getHomePageData();

        assertThat(result.getTopGainers()).isEmpty();
        assertThat(result.getTopLosers()).isEmpty();
    }
}
