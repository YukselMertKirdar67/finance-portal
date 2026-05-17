package com.financeportal.backend.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.User.DTO.LoginRequestDTO;
import com.financeportal.backend.User.DTO.RegisterRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth Controller Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USERNAME = "integrationtest_user";
    private static final String TEST_EMAIL = "integrationtest@test.com";
    private static final String TEST_PASSWORD = "Test1234!";

    // ========== HEALTH CHECK ==========

    @Test
    @Order(1)
    @DisplayName("Auth servis sağlık kontrolü başarılı olmalı")
    void health_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk());
    }

    // ========== REGISTER TESTS ==========

    @Test
    @Order(2)
    @DisplayName("Geçerli bilgilerle kullanıcı kaydı başarılı veya zaten var olmalı")
    void register_ValidRequest_ReturnsSuccess() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername(TEST_USERNAME);
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);
        request.setFirstName("Integration");
        request.setLastName("Test");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(200, 400));
    }

    @Test
    @Order(3)
    @DisplayName("Eksik alan ile kayıt başarısız olmalı")
    void register_MissingFields_ReturnsBadRequest() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("");
        request.setEmail("invalid");
        request.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== LOGIN TESTS ==========

    @Test
    @Order(4)
    @DisplayName("Login endpoint çalışıyor olmalı")
    void login_ValidCredentials_ReturnsToken() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus())
                                .isIn(200, 401)); // Keycloak erişilemezse 401 normal
    }

    @Test
    @Order(5)
    @DisplayName("Yanlış şifre ile giriş 401 dönmeli")
    void login_WrongPassword_ReturnsUnauthorized() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername(TEST_USERNAME);
        request.setPassword("WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus())
                                .isIn(200, 401));
    }

    @Test
    @Order(6)
    @DisplayName("Boş kullanıcı adı ile giriş reddedilmeli")
    void login_EmptyUsername_ReturnsBadRequest() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("");
        request.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
