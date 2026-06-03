package com.financeportal.backend.Instrument;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.DTO.*;
import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Instrument.Mapper.InstrumentMapper;
import com.financeportal.backend.Instrument.Repository.*;
import com.financeportal.backend.Instrument.Service.InstrumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Instrument Service Unit Tests")
class InstrumentServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentPriceRepository priceRepository;

    @Mock
    private PriceHistoryRepository historyRepository;

    @Mock
    private InstrumentMapper instrumentMapper;

    @InjectMocks
    private InstrumentServiceImpl instrumentService;

    private ForexInstrument testInstrument;
    private InstrumentPrice testPrice;
    private InstrumentResponseDTO testResponseDTO;
    private PriceDataDTO testPriceDataDTO;

    @BeforeEach
    void setUp() {
        testInstrument = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .currency("TRY")
                .exchange("TCMB")
                .build();
        testInstrument.setActive(true);

        testPrice = InstrumentPrice.builder()
                .instrument(testInstrument)
                .currentPrice(new BigDecimal("38.50"))
                .changeAmount(new BigDecimal("0.50"))
                .changePercent(new BigDecimal("1.32"))
                .timestamp(LocalDateTime.now())
                .build();

        testResponseDTO = InstrumentResponseDTO.builder()
                .id(1L)
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .type(InstrumentType.FOREX)
                .currency("TRY")
                .active(true)
                .build();

        testPriceDataDTO = PriceDataDTO.builder()
                .current(new BigDecimal("38.50"))
                .changePercent(new BigDecimal("1.32"))
                .currency("TRY")
                .timestamp(LocalDateTime.now())
                .build();

        when(instrumentMapper.toResponseDTO(any(), any())).thenReturn(testResponseDTO);
        when(instrumentMapper.toPriceDataDTO(any())).thenReturn(testPriceDataDTO);
    }

    @Test
    @DisplayName("ID ile enstrüman başarıyla getirilmeli")
    void getInstrumentById_Success() {
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(testPrice));

        InstrumentResponseDTO result = instrumentService.getInstrumentById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("USD/TRY");
        verify(instrumentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Var olmayan ID için exception fırlatılmalı")
    void getInstrumentById_NotFound_ThrowsException() {
        when(instrumentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instrumentService.getInstrumentById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sembol ile enstrüman başarıyla getirilmeli")
    void getInstrumentBySymbol_Success() {
        when(instrumentRepository.findBySymbol("USD/TRY")).thenReturn(Optional.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(testPrice));

        InstrumentResponseDTO result = instrumentService.getInstrumentBySymbol("USD/TRY");

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("USD/TRY");
    }

    @Test
    @DisplayName("Var olmayan sembol için exception fırlatılmalı")
    void getInstrumentBySymbol_NotFound_ThrowsException() {
        when(instrumentRepository.findBySymbol("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instrumentService.getInstrumentBySymbol("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tüm enstrümanlar sayfalı olarak getirilmeli")
    void getAllInstruments_ReturnsPage() {
        Page<BaseInstrument> page = new PageImpl<>(List.of(testInstrument));
        when(instrumentRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(testPrice));

        Page<InstrumentResponseDTO> result = instrumentService.getAllInstruments(
                PageRequest.of(0, 20));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Tipe göre enstrümanlar getirilmeli - FOREX")
    void getInstrumentsByType_Forex_ReturnsPage() {
        Page<BaseInstrument> page = new PageImpl<>(List.of(testInstrument));
        when(instrumentRepository.findAllForex(any(Pageable.class))).thenReturn(page);
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(testPrice));

        Page<InstrumentResponseDTO> result = instrumentService.getInstrumentsByType(
                InstrumentType.FOREX, PageRequest.of(0, 20));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Enstrüman arama sonuç döndürmeli")
    void searchInstruments_ReturnsResults() {
        Page<BaseInstrument> page = new PageImpl<>(List.of(testInstrument));
        when(instrumentRepository.searchInstruments(eq("USD"), any(Pageable.class)))
                .thenReturn(page);
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(testPrice));

        Page<InstrumentResponseDTO> result = instrumentService.searchInstruments(
                "USD", PageRequest.of(0, 20));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Anlık fiyat başarıyla getirilmeli")
    void getCurrentPrice_Success() {
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.of(testPrice));

        PriceDataDTO result = instrumentService.getCurrentPrice(1L);

        assertThat(result).isNotNull();
        assertThat(result.getCurrent()).isEqualByComparingTo(new BigDecimal("38.50"));
    }

    @Test
    @DisplayName("Fiyat verisi yoksa exception fırlatılmalı")
    void getCurrentPrice_NoPriceData_ThrowsException() {
        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(testInstrument))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> instrumentService.getCurrentPrice(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tarihsel fiyatlar başarıyla getirilmeli")
    void getHistoricalPrices_Success() {
        PriceHistory history = PriceHistory.builder()
                .instrument(testInstrument)
                .date(LocalDate.now().minusDays(1))
                .open(new BigDecimal("38.00"))
                .high(new BigDecimal("39.00"))
                .low(new BigDecimal("37.50"))
                .close(new BigDecimal("38.50"))
                .build();

        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
        when(historyRepository.findByInstrumentAndDateBetweenOrderByDateAsc(
                eq(testInstrument), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(history));
        when(instrumentMapper.toHistoricalPriceDTO(history)).thenReturn(
                HistoricalPriceDTO.builder()
                        .date(history.getDate())
                        .close(history.getClose())
                        .build());

        List<HistoricalPriceDTO> result = instrumentService.getHistoricalPrices(
                1L, LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }
}