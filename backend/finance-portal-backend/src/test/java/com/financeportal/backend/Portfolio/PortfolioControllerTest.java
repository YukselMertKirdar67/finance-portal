package com.financeportal.backend.Portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Portfolio.Controller.PortfolioController;
import com.financeportal.backend.Portfolio.DTO.*;
import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import com.financeportal.backend.Portfolio.Service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@ActiveProfiles("test")
@DisplayName("PortfolioController Testleri")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @Autowired
    private ObjectMapper objectMapper;

    private PortfolioDTO testPortfolioDTO;
    private CreatePortfolioRequestDTO createRequest;

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

        createRequest = new CreatePortfolioRequestDTO();
        createRequest.setName("Test Portföy");
        createRequest.setPortfolioType(PortfolioType.PERSONAL);
        createRequest.setCurrency("TRY");
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Portföy başarıyla oluşturulmalı")
    void createPortfolio_ReturnsCreated() throws Exception {
        when(portfolioService.createPortfolio(any())).thenReturn(testPortfolioDTO);

        mockMvc.perform(post("/api/v1/portfolios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Portföy"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Kullanıcı portföyleri getirilmeli")
    void getUserPortfolios_ReturnsOk() throws Exception {
        when(portfolioService.getUserPortfolios()).thenReturn(List.of(testPortfolioDTO));

        mockMvc.perform(get("/api/v1/portfolios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Portföy"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ID ile portföy getirilmeli")
    void getPortfolioById_ReturnsOk() throws Exception {
        when(portfolioService.getPortfolioById(1L)).thenReturn(testPortfolioDTO);

        mockMvc.perform(get("/api/v1/portfolios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Portföy silinmeli")
    void deletePortfolio_ReturnsNoContent() throws Exception {
        doNothing().when(portfolioService).deletePortfolio(1L);

        mockMvc.perform(delete("/api/v1/portfolios/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getUserPortfolios_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios"))
                .andExpect(status().isUnauthorized());
    }
}