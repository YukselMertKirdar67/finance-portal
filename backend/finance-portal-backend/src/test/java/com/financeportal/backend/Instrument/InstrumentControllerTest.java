package com.financeportal.backend.Instrument;

import com.financeportal.backend.Instrument.Controller.InstrumentController;
import com.financeportal.backend.Instrument.DTO.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Instrument.Service.InstrumentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstrumentController.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("InstrumentController Testleri")
class InstrumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstrumentService instrumentService;

    private InstrumentResponseDTO sampleInstrument;
    private PriceDataDTO samplePrice;
    private HistoricalPriceDTO sampleHistory;

    @BeforeEach
    void setUp() {
        samplePrice = PriceDataDTO.builder()
                .current(new BigDecimal("185.50"))
                .changePercent(new BigDecimal("1.25"))
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .build();

        sampleInstrument = InstrumentResponseDTO.builder()
                .id(1L)
                .name("Apple Inc.")
                .symbol("AAPL")
                .type(InstrumentType.STOCK)
                .currency("USD")
                .active(true)
                .currentPrice(samplePrice)
                .build();

        sampleHistory = HistoricalPriceDTO.builder()
                .date(LocalDate.of(2024, 1, 15))
                .close(new BigDecimal("182.00"))
                .open(new BigDecimal("180.00"))
                .high(new BigDecimal("186.00"))
                .low(new BigDecimal("179.00"))
                .volume(1000000L)
                .build();
    }

    @Test
    @Order(1)
    @WithMockUser(roles = "USER")
    @DisplayName("Tüm enstrümanları listele - başarılı")
    void getAllInstruments_ReturnsPage() throws Exception {
        Page<InstrumentResponseDTO> page = new PageImpl<>(List.of(sampleInstrument));
        when(instrumentService.getAllInstruments(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/instruments")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.content[0].name").value("Apple Inc."));
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "USER")
    @DisplayName("Tipe göre enstrümanları listele - STOCK")
    void getInstrumentsByType_ReturnsPage() throws Exception {
        Page<InstrumentResponseDTO> page = new PageImpl<>(List.of(sampleInstrument));
        when(instrumentService.getInstrumentsByType(eq(InstrumentType.STOCK), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/instruments/type/STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("AAPL"));
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "USER")
    @DisplayName("Enstrüman ara - sonuç döner")
    void searchInstruments_ReturnsResults() throws Exception {
        Page<InstrumentResponseDTO> page = new PageImpl<>(List.of(sampleInstrument));
        when(instrumentService.searchInstruments(eq("Apple"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/instruments/search")
                        .param("query", "Apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Apple Inc."));
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "USER")
    @DisplayName("ID ile enstrüman getir - başarılı")
    void getInstrumentById_ReturnsInstrument() throws Exception {
        when(instrumentService.getInstrumentById(1L)).thenReturn(sampleInstrument);

        mockMvc.perform(get("/api/instruments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "USER")
    @DisplayName("Sembol ile enstrüman getir - başarılı")
    void getInstrumentBySymbol_ReturnsInstrument() throws Exception {
        when(instrumentService.getInstrumentBySymbol("AAPL")).thenReturn(sampleInstrument);

        mockMvc.perform(get("/api/instruments/symbol")
                        .param("symbol", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "USER")
    @DisplayName("Anlık fiyat getir - başarılı")
    void getCurrentPrice_ReturnsPrice() throws Exception {
        when(instrumentService.getCurrentPrice(1L)).thenReturn(samplePrice);

        mockMvc.perform(get("/api/instruments/1/price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(185.50))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @Order(7)
    @WithMockUser(roles = "USER")
    @DisplayName("Geçmiş fiyatlar getir - başarılı")
    void getHistoricalPrices_ReturnsHistory() throws Exception {
        when(instrumentService.getHistoricalPrices(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(sampleHistory));

        mockMvc.perform(get("/api/instruments/1/history")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].close").value(182.00))
                .andExpect(jsonPath("$[0].volume").value(1000000));
    }

    @Test
    @Order(8)
    @DisplayName("Token olmadan erişim - 401")
    void getAllInstruments_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/instruments"))
                .andExpect(status().isUnauthorized());
    }
}
