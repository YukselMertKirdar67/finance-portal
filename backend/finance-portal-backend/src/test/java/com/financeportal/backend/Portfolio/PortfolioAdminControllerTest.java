package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Portfolio.Controller.PortfolioAdminController;
import com.financeportal.backend.Portfolio.DTO.PortfolioDTO;
import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import com.financeportal.backend.Portfolio.Service.PortfolioService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioAdminController.class)
@ActiveProfiles("test")
@DisplayName("PortfolioAdminController Unit Testleri")
class PortfolioAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    private PortfolioDTO testPortfolioDTO;

    @BeforeEach
    void setUp() {
        testPortfolioDTO = PortfolioDTO.builder()
                .id(1L)
                .name("Test Portföy")
                .portfolioType(PortfolioType.PERSONAL)
                .currency("TRY")
                .active(true)
                .totalValue(BigDecimal.ZERO)
                .totalInvested(BigDecimal.ZERO)
                .unrealizedPnL(BigDecimal.ZERO)
                .pnlPercent(BigDecimal.ZERO)
                .holdingCount(0)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin tüm portföyleri getirebilmeli")
    void getAllPortfolios_AdminRole_ReturnsOk() throws Exception {
        when(portfolioService.getAllPortfoliosAdmin(null)).thenReturn(List.of(testPortfolioDTO));

        mockMvc.perform(get("/api/v1/admin/portfolios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Portföy"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin userId ile portföyleri filtreleyebilmeli")
    void getAllPortfolios_WithUserId_ReturnsOk() throws Exception {
        when(portfolioService.getAllPortfoliosAdmin("test-user-id"))
                .thenReturn(List.of(testPortfolioDTO));

        mockMvc.perform(get("/api/v1/admin/portfolios")
                        .param("userId", "test-user-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Portföy"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin portföyü silebilmeli")
    void forceDeletePortfolio_AdminRole_ReturnsNoContent() throws Exception {
        doNothing().when(portfolioService).hardDeletePortfolio(1L);

        mockMvc.perform(delete("/api/v1/admin/portfolios/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Sistem istatistikleri getirilmeli")
    void getSystemStatistics_AdminRole_ReturnsOk() throws Exception {
        when(portfolioService.getSystemStatistics()).thenReturn(Map.of(
                "totalPortfolios", 10,
                "activePortfolios", 8
        ));

        mockMvc.perform(get("/api/v1/admin/portfolios/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPortfolios").value(10));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getAllPortfolios_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/portfolios"))
                .andExpect(status().isUnauthorized());
    }
}