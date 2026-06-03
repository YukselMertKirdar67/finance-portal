package com.financeportal.backend.Portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Portfolio.Controller.PortfolioTransactionController;
import com.financeportal.backend.Portfolio.DTO.*;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import com.financeportal.backend.Portfolio.Service.PortfolioTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

@WebMvcTest(PortfolioTransactionController.class)
@ActiveProfiles("test")
@DisplayName("PortfolioTransactionController Testleri")
class PortfolioTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioTransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private TransactionDTO testTransactionDTO;
    private CreateTransactionRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        testTransactionDTO = TransactionDTO.builder()
                .id(1L)
                .transactionType(TransactionType.BUY)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("38.50"))
                .build();

        createRequest = new CreateTransactionRequestDTO();
        createRequest.setInstrumentId(1L);
        createRequest.setTransactionType(TransactionType.BUY);
        createRequest.setQuantity(new BigDecimal("10"));
        createRequest.setPrice(new BigDecimal("38.50"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("İşlem başarıyla oluşturulmalı")
    void createTransaction_ReturnsCreated() throws Exception {
        when(transactionService.createTransaction(eq(1L), any())).thenReturn(testTransactionDTO);

        mockMvc.perform(post("/api/v1/portfolios/1/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("BUY"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("İşlem geçmişi sayfalı getirilmeli")
    void getTransactionHistory_ReturnsOk() throws Exception {
        Page<TransactionDTO> page = new PageImpl<>(List.of(testTransactionDTO));
        when(transactionService.getTransactionHistory(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/portfolios/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionType").value("BUY"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ID ile işlem getirilmeli")
    void getTransactionById_ReturnsOk() throws Exception {
        when(transactionService.getTransactionById(1L)).thenReturn(testTransactionDTO);

        mockMvc.perform(get("/api/v1/portfolios/1/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("İşlem silinmeli")
    void deleteTransaction_ReturnsNoContent() throws Exception {
        doNothing().when(transactionService).deleteTransaction(1L);

        mockMvc.perform(delete("/api/v1/portfolios/1/transactions/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getTransactionHistory_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/1/transactions"))
                .andExpect(status().isUnauthorized());
    }
}