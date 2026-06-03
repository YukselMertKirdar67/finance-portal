package com.financeportal.backend.Comparison;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Comparison Service Unit Tests")
class ComparisonServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentPriceRepository priceRepository;

    @Mock
    private PriceHistoryRepository historyRepository;

    @InjectMocks
    private ComparisonService comparisonService;

    private ForexInstrument instrument1;
    private ForexInstrument instrument2;
    private InstrumentPrice price1;
    private InstrumentPrice price2;
    private PriceHistory history1;
    private PriceHistory history2;

    @BeforeEach
    void setUp() {
        instrument1 = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .currency("TRY")
                .exchange("TCMB")
                .build();
        instrument1.setActive(true);

        instrument2 = ForexInstrument.builder()
                .symbol("EUR/TRY")
                .name("Euro")
                .currency("TRY")
                .exchange("TCMB")
                .build();
        instrument2.setActive(true);

        price1 = InstrumentPrice.builder()
                .instrument(instrument1)
                .currentPrice(new BigDecimal("38.50"))
                .changeAmount(new BigDecimal("0.50"))
                .changePercent(new BigDecimal("1.32"))
                .highPrice(new BigDecimal("39.00"))
                .lowPrice(new BigDecimal("38.00"))
                .timestamp(LocalDateTime.now())
                .build();

        price2 = InstrumentPrice.builder()
                .instrument(instrument2)
                .currentPrice(new BigDecimal("42.00"))
                .changeAmount(new BigDecimal("-0.30"))
                .changePercent(new BigDecimal("-0.71"))
                .highPrice(new BigDecimal("42.50"))
                .lowPrice(new BigDecimal("41.50"))
                .timestamp(LocalDateTime.now())
                .build();

        history1 = PriceHistory.builder()
                .instrument(instrument1)
                .date(LocalDate.now().minusDays(1))
                .open(new BigDecimal("38.00"))
                .high(new BigDecimal("38.80"))
                .low(new BigDecimal("37.90"))
                .close(new BigDecimal("38.50"))
                .build();

        history2 = PriceHistory.builder()
                .instrument(instrument2)
                .date(LocalDate.now().minusDays(1))
                .open(new BigDecimal("41.80"))
                .high(new BigDecimal("42.50"))
                .low(new BigDecimal("41.50"))
                .close(new BigDecimal("42.00"))
                .build();
    }

    @Test
    @DisplayName("İki enstrüman başarıyla karşılaştırılmalı")
    void compareInstruments_Success() {
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument1));
        when(instrumentRepository.findById(2L)).thenReturn(Optional.of(instrument2));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(instrument1))
                .thenReturn(Optional.of(price1));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(instrument2))
                .thenReturn(Optional.of(price2));
        when(historyRepository.findByInstrumentAndDateBetweenOrderByDateAsc(
                eq(instrument1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(history1));
        when(historyRepository.findByInstrumentAndDateBetweenOrderByDateAsc(
                eq(instrument2), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(history2));

        ComparisonDTO result = comparisonService.compareInstruments(1L, 2L, "1A");

        assertThat(result).isNotNull();
        assertThat(result.getInstrument1().getSymbol()).isEqualTo("USD/TRY");
        assertThat(result.getInstrument2().getSymbol()).isEqualTo("EUR/TRY");
        assertThat(result.getPeriod()).isEqualTo("1A");
        assertThat(result.getMetrics()).isNotNull();
    }

    @Test
    @DisplayName("Var olmayan enstrüman için exception fırlatılmalı")
    void compareInstruments_Instrument1NotFound_ThrowsException() {
        when(instrumentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comparisonService.compareInstruments(999L, 2L, "1A"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("İkinci enstrüman bulunamazsa exception fırlatılmalı")
    void compareInstruments_Instrument2NotFound_ThrowsException() {
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument1));
        when(instrumentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comparisonService.compareInstruments(1L, 999L, "1A"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tarihsel veri yoksa boş data points dönmeli")
    void compareInstruments_NoHistoricalData_EmptyDataPoints() {
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument1));
        when(instrumentRepository.findById(2L)).thenReturn(Optional.of(instrument2));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(instrument1))
                .thenReturn(Optional.of(price1));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(instrument2))
                .thenReturn(Optional.of(price2));
        when(historyRepository.findByInstrumentAndDateBetweenOrderByDateAsc(
                any(), any(), any())).thenReturn(List.of());

        ComparisonDTO result = comparisonService.compareInstruments(1L, 2L, "1A");

        assertThat(result.getHistoricalData()).isEmpty();
    }

    @Test
    @DisplayName("Tüm period seçenekleri desteklenmeli")
    void compareInstruments_AllPeriods_Success() {
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument1));
        when(instrumentRepository.findById(2L)).thenReturn(Optional.of(instrument2));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(any()))
                .thenReturn(Optional.of(price1));
        when(historyRepository.findByInstrumentAndDateBetweenOrderByDateAsc(
                any(), any(), any())).thenReturn(List.of());

        for (String period : List.of("1H", "1A", "3A", "6A", "1Y")) {
            ComparisonDTO result = comparisonService.compareInstruments(1L, 2L, period);
            assertThat(result.getPeriod()).isEqualTo(period);
        }
    }
}
