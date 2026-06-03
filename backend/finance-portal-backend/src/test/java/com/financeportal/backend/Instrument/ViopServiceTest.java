package com.financeportal.backend.Instrument;

import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Repository.*;
import com.financeportal.backend.Instrument.Service.ViopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ViopService Unit Tests")
class ViopServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentPriceRepository instrumentPriceRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ViopService viopService;

    private ViopInstrument testViop;
    private InstrumentPrice testPrice;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(viopService, "oneEndeksUrl",
                "https://www.isyatirim.com.tr/_layouts/15/Isyatirim.Website/Common/Data.aspx/OneEndeks?endeks=");
        ReflectionTestUtils.setField(viopService, "historicalUrl",
                "https://www.isyatirim.com.tr/_layouts/15/Isyatirim.Website/Common/Data.aspx/IndexHistoricalAll");

        testViop = ViopInstrument.builder()
                .symbol("F_AKBNK0626")
                .name("AKBNK Haziran 2026 Vadeli")
                .exchange("VIOP")
                .currency("TRY")
                .active(true)
                .underlyingAsset("AKBNK")
                .contractType("FUTURES")
                .expiryDate(LocalDate.of(2026, 6, 30))
                .build();

        testPrice = InstrumentPrice.builder()
                .instrument(testViop)
                .currentPrice(new BigDecimal("45.00"))
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Vadesi geçmemiş kontrat pasife alınmamalı")
    void deactivateExpiredContracts_NotExpired_StaysActive() {
        when(instrumentRepository.findByExchangeAndActiveTrue("VIOP"))
                .thenReturn(List.of(testViop));

        viopService.deactivateExpiredContracts();

        assertThat(testViop.isActive()).isTrue();
        verify(instrumentRepository, never()).save(testViop);
    }

    @Test
    @DisplayName("Vadesi geçmiş kontrat pasife alınmalı")
    void deactivateExpiredContracts_Expired_Deactivated() {
        testViop.setExpiryDate(LocalDate.now().minusDays(1));
        when(instrumentRepository.findByExchangeAndActiveTrue("VIOP"))
                .thenReturn(List.of(testViop));
        when(instrumentRepository.save(testViop)).thenReturn(testViop);

        viopService.deactivateExpiredContracts();

        assertThat(testViop.isActive()).isFalse();
        verify(instrumentRepository, times(1)).save(testViop);
    }

    @Test
    @DisplayName("VİOP fiyatı başarıyla güncellenmeli")
    void fetchViopPrices_Success() {
        when(instrumentRepository.findByExchangeAndActiveTrue("VIOP"))
                .thenReturn(List.of(testViop));

        Map<String, Object> data = Map.of(
                "last", 45.50,
                "open", 45.00,
                "high", 46.00,
                "low", 44.50,
                "dayClose", 45.00,
                "volume", 1000
        );

        when(restTemplate.getForObject(anyString(), eq(List.class)))
                .thenReturn(List.of(data));
        when(instrumentPriceRepository.findTopByInstrumentOrderByTimestampDesc(testViop))
                .thenReturn(Optional.of(testPrice));
        when(instrumentPriceRepository.save(any())).thenReturn(testPrice);

        viopService.fetchViopPrices();

        verify(instrumentPriceRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("API boş response döndürürse fiyat güncellenmemeli")
    void fetchViopPrices_EmptyResponse_NotUpdated() {
        when(instrumentRepository.findByExchangeAndActiveTrue("VIOP"))
                .thenReturn(List.of(testViop));
        when(restTemplate.getForObject(anyString(), eq(List.class)))
                .thenReturn(List.of());

        viopService.fetchViopPrices();

        verify(instrumentPriceRepository, never()).save(any());
    }

    @Test
    @DisplayName("API hata verirse diğer kontratlar etkilenmemeli")
    void fetchViopPrices_ApiError_ContinuesWithOthers() {
        ViopInstrument viop2 = ViopInstrument.builder()
                .symbol("F_GARAN0626")
                .name("GARAN Haziran 2026 Vadeli")
                .exchange("VIOP")
                .currency("TRY")
                .active(true)
                .expiryDate(LocalDate.of(2026, 6, 30))
                .build();

        when(instrumentRepository.findByExchangeAndActiveTrue("VIOP"))
                .thenReturn(List.of(testViop, viop2));
        when(restTemplate.getForObject(contains("F_AKBNK"), eq(List.class)))
                .thenThrow(new RuntimeException("API Error"));
        when(restTemplate.getForObject(contains("F_GARAN"), eq(List.class)))
                .thenReturn(List.of(Map.of("last", 25.0, "open", 24.5,
                        "high", 25.5, "low", 24.0, "dayClose", 24.5, "volume", 500)));
        when(instrumentPriceRepository.findTopByInstrumentOrderByTimestampDesc(viop2))
                .thenReturn(Optional.empty());
        when(instrumentPriceRepository.save(any())).thenReturn(testPrice);

        assertThatNoException().isThrownBy(() -> viopService.fetchViopPrices());
    }
}
