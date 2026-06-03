package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.ForexInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Service.TcmbService;
import com.financeportal.backend.Portfolio.DTO.AssetAllocationDTO;
import com.financeportal.backend.Portfolio.DTO.HoldingDTO;
import com.financeportal.backend.Portfolio.Entity.PortfolioHolding;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapper;
import com.financeportal.backend.Portfolio.Repository.PortfolioHoldingRepository;
import com.financeportal.backend.Portfolio.Service.PortfolioCalculationService;
import com.financeportal.backend.Portfolio.Service.PortfolioHoldingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PortfolioHoldingService Unit Testleri")
class PortfolioHoldingServiceTest {

    @Mock private PortfolioHoldingRepository holdingRepository;
    @Mock private InstrumentPriceRepository priceRepository;
    @Mock private PortfolioCalculationService calculationService;
    @Mock private PortfolioMapper portfolioMapper;
    @Mock private TcmbService tcmbService;

    @InjectMocks
    private PortfolioHoldingServiceImpl holdingService;

    private ForexInstrument testInstrument;
    private PortfolioHolding testHolding;
    private HoldingDTO testHoldingDTO;

    @BeforeEach
    void setUp() {
        testInstrument = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .currency("TRY")
                .exchange("TCMB")
                .build();
        testInstrument.setId(1L);

        testHolding = new PortfolioHolding();
        testHolding.setId(1L);
        testHolding.setInstrument(testInstrument);
        testHolding.setQuantity(new BigDecimal("10"));
        testHolding.setAverageBuyPrice(new BigDecimal("38.00"));
        testHolding.setCurrency("TRY");

        testHoldingDTO = HoldingDTO.builder()
                .holdingId(1L)                          // id → holdingId
                .instrumentSymbol("USD/TRY")
                .quantity(new BigDecimal("10"))
                .averageBuyPrice(new BigDecimal("38.00"))
                .currentValue(new BigDecimal("385.00"))
                .unrealizedPnL(new BigDecimal("5.00"))
                .pnlPercent(new BigDecimal("1.32"))
                .instrumentType("FOREX")
                .build();

        when(priceRepository.findTopByInstrumentOrderByTimestampDesc(any()))
                .thenReturn(Optional.of(InstrumentPrice.builder()
                        .currentPrice(new BigDecimal("38.50"))
                        .build()));
        when(tcmbService.getExchangeRate(anyString())).thenReturn(new BigDecimal("1.00"));
        when(tcmbService.convertFromTRY(any(), anyString())).thenAnswer(i -> i.getArgument(0));
        when(portfolioMapper.toHoldingDTO(any(), any())).thenReturn(testHoldingDTO);
        when(calculationService.calculateTotalInvestment(any(), any())).thenReturn(new BigDecimal("380.00"));
        when(calculationService.calculateCurrentValue(any(), any())).thenReturn(new BigDecimal("385.00"));
        when(calculationService.calculateUnrealizedPnL(any(), any(), any())).thenReturn(new BigDecimal("5.00"));
        when(calculationService.calculatePnLPercent(any(), any())).thenReturn(new BigDecimal("1.32"));
    }

    @Test
    @DisplayName("Portföy holdingleri başarıyla getirilmeli")
    void getHoldingsByPortfolioId_ReturnsHoldings() {
        when(holdingRepository.findByPortfolioIdWithInstrument(1L))
                .thenReturn(List.of(testHolding));

        List<HoldingDTO> result = holdingService.getHoldingsByPortfolioId(1L);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstrumentSymbol()).isEqualTo("USD/TRY");
    }

    @Test
    @DisplayName("Boş portföy için boş liste dönmeli")
    void getHoldingsByPortfolioId_EmptyPortfolio_ReturnsEmptyList() {
        when(holdingRepository.findByPortfolioIdWithInstrument(1L))
                .thenReturn(List.of());

        List<HoldingDTO> result = holdingService.getHoldingsByPortfolioId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ID ile holding başarıyla getirilmeli")
    void getHoldingById_Success() {
        when(holdingRepository.findById(1L)).thenReturn(Optional.of(testHolding));

        HoldingDTO result = holdingService.getHoldingById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getInstrumentSymbol()).isEqualTo("USD/TRY");
    }

    @Test
    @DisplayName("Var olmayan holding için exception fırlatılmalı")
    void getHoldingById_NotFound_ThrowsException() {
        when(holdingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holdingService.getHoldingById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Aktif holdingler getirilmeli")
    void getActiveHoldings_ReturnsActiveHoldings() {
        when(holdingRepository.findActiveHoldingsByPortfolioId(1L))
                .thenReturn(List.of(testHolding));

        List<HoldingDTO> result = holdingService.getActiveHoldings(1L);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Holding başarıyla silinmeli")
    void deleteHolding_Success() {
        when(holdingRepository.existsById(1L)).thenReturn(true);
        doNothing().when(holdingRepository).deleteById(1L);

        assertThatNoException().isThrownBy(() -> holdingService.deleteHolding(1L));

        verify(holdingRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Var olmayan holding silinmeye çalışılınca exception fırlatılmalı")
    void deleteHolding_NotFound_ThrowsException() {
        when(holdingRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> holdingService.deleteHolding(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sıfır miktarlı holdingler temizlenmeli")
    void deleteZeroQuantityHoldings_DeletesZeroQuantity() {
        PortfolioHolding zeroHolding = new PortfolioHolding();
        zeroHolding.setQuantity(BigDecimal.ZERO);
        zeroHolding.setInstrument(testInstrument);

        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(zeroHolding, testHolding));

        int result = holdingService.deleteZeroQuantityHoldings(1L);

        assertThat(result).isEqualTo(1);
        verify(holdingRepository, times(1)).delete(zeroHolding);
        verify(holdingRepository, never()).delete(testHolding);
    }

    @Test
    @DisplayName("Toplam yatırım hesaplanmalı")
    void calculateTotalInvestment_ReturnsValue() {
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(testHolding));

        BigDecimal result = holdingService.calculateTotalInvestment(1L);

        assertThat(result).isNotNull();
        assertThat(result).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Varlık dağılımı hesaplanmalı")
    void getAssetAllocation_ReturnsAllocation() {
        when(holdingRepository.findByPortfolioIdWithInstrument(1L))
                .thenReturn(List.of(testHolding));

        List<AssetAllocationDTO> result = holdingService.getAssetAllocation(1L);

        assertThat(result).isNotNull();
    }
}
