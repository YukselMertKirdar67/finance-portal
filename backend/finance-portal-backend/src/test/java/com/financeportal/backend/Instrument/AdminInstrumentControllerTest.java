package com.financeportal.backend.Instrument;

import com.financeportal.backend.Instrument.Controller.AdminInstrumentController;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import com.financeportal.backend.Instrument.Entity.ForexInstrument;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Service.*;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminInstrumentController.class)
@ActiveProfiles("test")
@DisplayName("AdminInstrumentController Unit Testleri")
class AdminInstrumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private TcmbService tcmbService;
    @MockBean private TcmbEvdsService tcmbEvdsService;
    @MockBean private YahooFinanceService yahooFinanceService;
    @MockBean private ViopService viopService;
    @MockBean private InstrumentRepository instrumentRepository;

    private InstrumentPrice testPrice;

    @BeforeEach
    void setUp() {
        ForexInstrument instrument = ForexInstrument.builder()
                .symbol("USD/TRY").name("Amerikan Doları")
                .currency("TRY").exchange("TCMB").build();
        instrument.setId(1L);

        testPrice = InstrumentPrice.builder()
                .instrument(instrument)
                .currentPrice(new BigDecimal("38.50"))
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Güncelleme durumu getirilmeli")
    void getUpdateStatus_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/admin/instruments/update-status"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("TCMB kurları güncellenmeli")
    void updateTcmbRates_ReturnsOk() throws Exception {
        when(tcmbService.fetchDailyRates()).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/admin/instruments/update-tcmb")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("TCMB güncellemesi exception durumunda 500 dönmeli")
    void updateTcmbRates_Exception_Returns500() throws Exception {
        when(tcmbService.fetchDailyRates()).thenThrow(new RuntimeException("API error"));

        mockMvc.perform(post("/api/v1/admin/instruments/update-tcmb")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("TR tahvil güncellenmeli")
    void updateTrBondYields_ReturnsOk() throws Exception {
        when(tcmbEvdsService.fetchBondYields()).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/admin/instruments/update-tr-bonds")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ABD tahvil güncellenmeli")
    void updateBondYields_ReturnsOk() throws Exception {
        when(yahooFinanceService.updateBonds()).thenReturn(4);

        mockMvc.perform(post("/api/v1/admin/instruments/update-bonds")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedCount").value(4));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ABD hisseleri güncellenmeli")
    void updateUsStocks_ReturnsOk() throws Exception {
        when(yahooFinanceService.updateUsStocks()).thenReturn(10);

        mockMvc.perform(post("/api/v1/admin/instruments/update-us-stocks")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedCount").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("BIST hisseleri güncellenmeli")
    void updateBistStocks_ReturnsOk() throws Exception {
        when(yahooFinanceService.updateBistStocks()).thenReturn(15);

        mockMvc.perform(post("/api/v1/admin/instruments/update-bist")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedCount").value(15));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Kripto paralar güncellenmeli")
    void updateCryptos_ReturnsOk() throws Exception {
        when(yahooFinanceService.updateCryptos()).thenReturn(11);

        mockMvc.perform(post("/api/v1/admin/instruments/update-crypto")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedCount").value(11));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Kıymetli metaller güncellenmeli")
    void updatePreciousMetals_ReturnsOk() throws Exception {
        when(yahooFinanceService.updatePreciousMetals()).thenReturn(4);

        mockMvc.perform(post("/api/v1/admin/instruments/update-precious")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedCount").value(4));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ETF'ler güncellenmeli")
    void updateEtfs_ReturnsOk() throws Exception {
        when(yahooFinanceService.updateEtfs()).thenReturn(10);

        mockMvc.perform(post("/api/v1/admin/instruments/update-etfs")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedCount").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Belirli sembol güncellenmeli")
    void updateSymbol_ReturnsOk() throws Exception {
        when(yahooFinanceService.fetchQuote("AAPL", "AAPL")).thenReturn(testPrice);

        mockMvc.perform(post("/api/v1/admin/instruments/update-symbol/AAPL/AAPL")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Sembol güncellenemediğinde 400 dönmeli")
    void updateSymbol_NullPrice_ReturnsBadRequest() throws Exception {
        when(yahooFinanceService.fetchQuote("INVALID", "INVALID")).thenReturn(null);

        mockMvc.perform(post("/api/v1/admin/instruments/update-symbol/INVALID/INVALID")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("API istatistikleri getirilmeli")
    void getApiStats_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/admin/instruments/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Enstrüman detayları güncellenmeli")
    void updateInstrumentDetails_ReturnsOk() throws Exception {
        doNothing().when(yahooFinanceService).updateExistingInstrumentDetails();

        mockMvc.perform(post("/api/v1/admin/instruments/update-instrument-details")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getUpdateStatus_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/instruments/update-status"))
                .andExpect(status().isUnauthorized());
    }
}
