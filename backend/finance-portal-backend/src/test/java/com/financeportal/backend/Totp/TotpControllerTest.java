package com.financeportal.backend.Totp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TotpController.class)
@ActiveProfiles("test")
@DisplayName("TotpController Unit Testleri")
class TotpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TotpService totpService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("TOTP kurulumu başarıyla yapılmalı")
    void setupTotp_ReturnsOk() throws Exception {
        when(totpService.setupTotp(anyString(), anyString())).thenReturn(Map.of(
                "secret", "JBSWY3DPEHPK3PXP",
                "qrCode", "data:image/png;base64,abc123"
        ));

        mockMvc.perform(post("/api/v1/totp/setup")
                        .with(jwt().jwt(j -> j
                                .subject("test-keycloak-id")
                                .claim("email", "test@test.com")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("JBSWY3DPEHPK3PXP"));
    }

    @Test
    @DisplayName("Geçerli kod ile TOTP aktif edilmeli")
    void verifySetup_ValidCode_ReturnsOk() throws Exception {
        when(totpService.verifyAndActivateTotp(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/v1/totp/verify-setup")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Geçersiz kod ile TOTP aktivasyonu 400 dönmeli")
    void verifySetup_InvalidCode_ReturnsBadRequest() throws Exception {
        when(totpService.verifyAndActivateTotp(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/v1/totp/verify-setup")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Login TOTP doğrulaması başarılı olmalı")
    void verifyLogin_ValidCode_ReturnsOk() throws Exception {
        when(totpService.verifyTotpCode(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/v1/totp/verify-login")
                        .with(jwt())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keycloakId\":\"test-id\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Login TOTP doğrulaması başarısız olursa 400 dönmeli")
    void verifyLogin_InvalidCode_ReturnsBadRequest() throws Exception {
        when(totpService.verifyTotpCode(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/v1/totp/verify-login")
                        .with(jwt())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keycloakId\":\"test-id\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("TOTP durumu aktif olarak getirilmeli")
    void getTotpStatus_Enabled_ReturnsOk() throws Exception {
        when(totpService.isTotpEnabled(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/v1/totp/status")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("TOTP devre dışı bırakılmalı")
    void disableTotp_ReturnsOk() throws Exception {
        doNothing().when(totpService).disableTotp(anyString());

        mockMvc.perform(delete("/api/v1/totp/disable")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void setupTotp_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/totp/setup")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
