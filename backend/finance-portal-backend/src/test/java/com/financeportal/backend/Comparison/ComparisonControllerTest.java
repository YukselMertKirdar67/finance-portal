package com.financeportal.backend.Comparison;

import com.financeportal.backend.Instrument.Enum.InstrumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComparisonController.class)
@ActiveProfiles("test")
@DisplayName("ComparisonController Testleri")
class ComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComparisonService comparisonService;

    private ComparisonDTO testComparisonDTO;

    @BeforeEach
    void setUp() {
        testComparisonDTO = ComparisonDTO.builder()
                .instrument1(ComparisonDTO.InstrumentInfo.builder()
                        .id(1L).symbol("USD/TRY").name("Amerikan Doları")
                        .type(InstrumentType.FOREX.name()).currency("TRY")
                        .currentPrice(new BigDecimal("38.50")).build())
                .instrument2(ComparisonDTO.InstrumentInfo.builder()
                        .id(2L).symbol("EUR/TRY").name("Euro")
                        .type(InstrumentType.FOREX.name()).currency("TRY")
                        .currentPrice(new BigDecimal("42.00")).build())
                .historicalData(List.of())
                .metrics(ComparisonDTO.PerformanceMetrics.builder()
                        .instrument1Metrics(ComparisonDTO.MetricData.builder()
                                .periodChange(new BigDecimal("1.32"))
                                .volatility(new BigDecimal("0.50"))
                                .highestPrice(new BigDecimal("39.00"))
                                .lowestPrice(new BigDecimal("37.00"))
                                .priceRange(new BigDecimal("2.00"))
                                .build())
                        .instrument2Metrics(ComparisonDTO.MetricData.builder()
                                .periodChange(new BigDecimal("-0.71"))
                                .volatility(new BigDecimal("0.30"))
                                .highestPrice(new BigDecimal("43.00"))
                                .lowestPrice(new BigDecimal("41.00"))
                                .priceRange(new BigDecimal("2.00"))
                                .build())
                        .build())
                .period("1A")
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("İki enstrüman karşılaştırması başarıyla yapılmalı")
    void compareInstruments_ReturnsOk() throws Exception {
        when(comparisonService.compareInstruments(eq(1L), eq(2L), anyString()))
                .thenReturn(testComparisonDTO);

        mockMvc.perform(get("/api/v1/comparison")
                        .param("id1", "1")
                        .param("id2", "2")
                        .param("period", "1A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instrument1.symbol").value("USD/TRY"))
                .andExpect(jsonPath("$.instrument2.symbol").value("EUR/TRY"))
                .andExpect(jsonPath("$.period").value("1A"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Varsayılan period ile karşılaştırma yapılmalı")
    void compareInstruments_DefaultPeriod_ReturnsOk() throws Exception {
        when(comparisonService.compareInstruments(eq(1L), eq(2L), eq("1A")))
                .thenReturn(testComparisonDTO);

        mockMvc.perform(get("/api/v1/comparison")
                        .param("id1", "1")
                        .param("id2", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void compareInstruments_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/comparison")
                        .param("id1", "1")
                        .param("id2", "2"))
                .andExpect(status().isUnauthorized());
    }
}
