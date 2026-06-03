package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Portfolio.Controller.PortfolioHoldingController;
import com.financeportal.backend.Portfolio.DTO.AssetAllocationDTO;
import com.financeportal.backend.Portfolio.DTO.HoldingDTO;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Service.PortfolioHoldingService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioHoldingController.class)
@ActiveProfiles("test")
@DisplayName("PortfolioHoldingController Unit Testleri")
class PortfolioHoldingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioHoldingService holdingService;

    @MockBean
    private PortfolioRepository portfolioRepository;

    private HoldingDTO testHoldingDTO;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Portföy holdingleri getirilmeli")
    void getHoldings_ReturnsOk() throws Exception {
        when(holdingService.getHoldingsByPortfolioId(1L)).thenReturn(List.of(testHoldingDTO));

        mockMvc.perform(get("/api/v1/portfolios/1/holdings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instrumentSymbol").value("USD/TRY"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Aktif holdingler getirilmeli")
    void getActiveHoldings_ReturnsOk() throws Exception {
        when(holdingService.getActiveHoldings(1L)).thenReturn(List.of(testHoldingDTO));

        mockMvc.perform(get("/api/v1/portfolios/1/holdings/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instrumentSymbol").value("USD/TRY"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("En yüksek değerli holdingler getirilmeli")
    void getTopHoldings_ReturnsOk() throws Exception {
        when(holdingService.getTopHoldingsByValue(1L, 10)).thenReturn(List.of(testHoldingDTO));

        mockMvc.perform(get("/api/v1/portfolios/1/holdings/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instrumentSymbol").value("USD/TRY"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ID ile holding getirilmeli")
    void getHoldingById_ReturnsOk() throws Exception {
        when(holdingService.getHoldingById(1L)).thenReturn(testHoldingDTO);

        mockMvc.perform(get("/api/v1/portfolios/1/holdings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdingId").value(1));  // $.id → $.holdingId
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Varlık dağılımı getirilmeli")
    void getAssetAllocation_ReturnsOk() throws Exception {
        AssetAllocationDTO allocationDTO = AssetAllocationDTO.builder()
                .instrumentType("FOREX")
                .totalValue(new BigDecimal("385.00"))
                .percentage(new BigDecimal("100.00"))
                .count(1)
                .build();

        when(portfolioRepository.findById(1L)).thenReturn(Optional.empty());
        when(holdingService.getAssetAllocation(1L, "TRY")).thenReturn(List.of(allocationDTO));

        mockMvc.perform(get("/api/v1/portfolios/1/holdings/asset-allocation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instrumentType").value("FOREX"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Holding silinmeli")
    void deleteHolding_ReturnsNoContent() throws Exception {
        doNothing().when(holdingService).deleteHolding(1L);

        mockMvc.perform(delete("/api/v1/portfolios/1/holdings/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Sıfır miktarlı holdingler temizlenmeli")
    void deleteZeroQuantityHoldings_ReturnsOk() throws Exception {
        when(holdingService.deleteZeroQuantityHoldings(1L)).thenReturn(2);

        mockMvc.perform(delete("/api/v1/portfolios/1/holdings/cleanup")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getHoldings_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/1/holdings"))
                .andExpect(status().isUnauthorized());
    }
}
