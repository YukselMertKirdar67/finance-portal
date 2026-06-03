package com.financeportal.backend.PriceAlert;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(PriceAlertController.class)
@ActiveProfiles("test")
@DisplayName("PriceAlertController Testleri")
class PriceAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceAlertService priceAlertService;

    @Autowired
    private ObjectMapper objectMapper;

    private PriceAlertDTO testAlertDTO;
    private CreatePriceAlertRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        testAlertDTO = PriceAlertDTO.builder()
                .id(1L)
                .instrumentSymbol("USD/TRY")
                .targetPrice(new BigDecimal("40.00"))
                .condition(AlertCondition.ABOVE)
                .active(true)
                .triggered(false)
                .build();

        createRequest = new CreatePriceAlertRequestDTO();
        createRequest.setInstrumentId(1L);
        createRequest.setTargetPrice(new BigDecimal("40.00"));
        createRequest.setCondition(AlertCondition.ABOVE);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Fiyat alarmı başarıyla oluşturulmalı")
    void createAlert_ReturnsOk() throws Exception {
        when(priceAlertService.createAlert(any())).thenReturn(testAlertDTO);

        mockMvc.perform(post("/api/v1/price-alerts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instrumentSymbol").value("USD/TRY"))
                .andExpect(jsonPath("$.targetPrice").value(40.00));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Tüm alarmlar getirilmeli")
    void getUserAlerts_ReturnsOk() throws Exception {
        when(priceAlertService.getUserAlerts()).thenReturn(List.of(testAlertDTO));

        mockMvc.perform(get("/api/v1/price-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instrumentSymbol").value("USD/TRY"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Aktif alarmlar getirilmeli")
    void getActiveUserAlerts_ReturnsOk() throws Exception {
        when(priceAlertService.getActiveUserAlerts()).thenReturn(List.of(testAlertDTO));

        mockMvc.perform(get("/api/v1/price-alerts/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Alarm başarıyla silinmeli")
    void deleteAlert_ReturnsOk() throws Exception {
        doNothing().when(priceAlertService).deleteAlert(1L);

        mockMvc.perform(delete("/api/v1/price-alerts/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getUserAlerts_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/price-alerts"))
                .andExpect(status().isUnauthorized());
    }
}