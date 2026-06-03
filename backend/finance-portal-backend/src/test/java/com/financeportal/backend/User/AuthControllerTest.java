package com.financeportal.backend.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.Email.EmailService;
import com.financeportal.backend.User.Controller.AuthController;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Service.AuthService;
import com.financeportal.backend.User.Service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("AuthController Unit Testleri")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@test.com");
        registerRequest.setPassword("Test1234!");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test1234!");
    }

    @Test
    @WithMockUser
    @DisplayName("Auth servis sağlık kontrolü başarılı olmalı")
    void health_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("Geçerli bilgilerle kayıt başarılı olmalı")
    void register_ValidRequest_ReturnsOk() throws Exception {
        RegisterResponseDTO response = RegisterResponseDTO.builder()
                .success(true)
                .message("Registration successful!")
                .userId("test-user-id")
                .build();

        when(authService.registerUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Kayıt başarısız olduğunda 400 dönmeli")
    void register_Failed_ReturnsBadRequest() throws Exception {
        RegisterResponseDTO response = RegisterResponseDTO.builder()
                .success(false)
                .message("Username already exists")
                .build();

        when(authService.registerUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("Eksik alan ile kayıt 400 dönmeli")
    void register_MissingFields_ReturnsBadRequest() throws Exception {
        RegisterRequestDTO invalid = new RegisterRequestDTO();
        invalid.setUsername("");
        invalid.setEmail("invalid-email");
        invalid.setPassword("123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("Başarılı login token dönmeli")
    void login_Success_ReturnsToken() throws Exception {
        LoginResponseDTO response = LoginResponseDTO.builder()
                .success(true)
                .message("Login successful")
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .username("testuser")
                .build();

        when(authService.login(any())).thenReturn(response);
        when(jwtDecoder.decode(anyString())).thenThrow(new RuntimeException("JWT decode skipped"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accessToken").value("test-access-token"));
    }

    @Test
    @WithMockUser
    @DisplayName("Başarısız login 401 dönmeli")
    void login_Failed_ReturnsUnauthorized() throws Exception {
        LoginResponseDTO response = LoginResponseDTO.builder()
                .success(false)
                .message("Invalid username or password")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("2FA gerektiğinde 200 ve 2FA_REQUIRED mesajı dönmeli")
    void login_TwoFactorRequired_ReturnsOk() throws Exception {
        LoginResponseDTO response = LoginResponseDTO.builder()
                .success(false)
                .message("2FA_REQUIRED")
                .keycloakId("test-user-id")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("2FA_REQUIRED"));
    }

    @Test
    @WithMockUser
    @DisplayName("Boş username ile login 400 dönmeli")
    void login_EmptyUsername_ReturnsBadRequest() throws Exception {
        LoginRequestDTO invalid = new LoginRequestDTO();
        invalid.setUsername("");
        invalid.setPassword("Test1234!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("Şifre sıfırlama maili başarıyla gönderilmeli")
    void forgotPassword_ReturnsOk() throws Exception {
        PasswordResetResponseDTO response = PasswordResetResponseDTO.builder()
                .success(true)
                .message("If the email exists, a password reset link has been sent.")
                .build();

        when(authService.sendPasswordResetEmail(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Email doğrulama kontrolü başarılı olmalı")
    void checkEmailVerification_ReturnsOk() throws Exception {
        EmailVerificationResponseDTO response = EmailVerificationResponseDTO.builder()
                .success(true)
                .message("Email is verified")
                .emailVerified(true)
                .build();

        when(authService.checkEmailVerification(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/check-email-verification")
                        .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Geçerli token ile email doğrulanmalı")
    void verifyEmail_ValidToken_ReturnsOk() throws Exception {
        when(emailService.verifyEmail("valid-token")).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Geçersiz token ile email doğrulama 400 dönmeli")
    void verifyEmail_InvalidToken_ReturnsBadRequest() throws Exception {
        when(emailService.verifyEmail("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("Logout başarıyla tamamlanmalı")
    void logout_ReturnsOk() throws Exception {
        LogoutResponseDTO response = LogoutResponseDTO.builder()
                .success(true)
                .message("Logout successful")
                .build();

        when(authService.logout(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"test-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}