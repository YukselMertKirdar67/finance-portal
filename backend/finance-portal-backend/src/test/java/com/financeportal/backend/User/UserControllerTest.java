package com.financeportal.backend.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeportal.backend.User.Controller.UserController;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Service.AuthService;
import com.financeportal.backend.User.Service.UserService;
import com.financeportal.backend.User.UserMapper;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@DisplayName("UserController Unit Testleri")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private MeResponseDTO testMeResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .keycloakId("test-keycloak-id")
                .username("testuser")
                .email("test@test.com")
                .enabled(true)
                .build();

        testMeResponse = new MeResponseDTO(
                "test-keycloak-id",
                "testuser",
                "test@test.com",
                List.of("USER")
        );

        when(userService.getOrCreateUser(any())).thenReturn(testUser);
        when(userMapper.toMeResponseDTO(any(), any())).thenReturn(testMeResponse);
    }

    @Test
    @DisplayName("Me endpoint JWT ile kullanıcı bilgisi dönmeli")
    void me_WithJwt_ReturnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .with(jwt().jwt(j -> j
                                .subject("test-keycloak-id")
                                .claim("preferred_username", "testuser")
                                .claim("email", "test@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("Token olmadan me endpoint 401 dönmeli")
    void me_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ping endpoint başarıyla çalışmalı")
    void ping_WithJwt_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/me/ping")
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Şifre değiştirme başarıyla tamamlanmalı")
    void changePassword_Success_ReturnsOk() throws Exception {
        ChangePasswordResponseDTO response = ChangePasswordResponseDTO.builder()
                .success(true)
                .message("Şifreniz başarıyla değiştirildi")
                .build();

        when(authService.changePassword(anyString(), any())).thenReturn(response);

        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("NewPass123!");
        request.setConfirmPassword("NewPass123!");

        mockMvc.perform(post("/api/v1/me/change-password")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Şifre değiştirme başarısız olduğunda 400 dönmeli")
    void changePassword_Failed_ReturnsBadRequest() throws Exception {
        ChangePasswordResponseDTO response = ChangePasswordResponseDTO.builder()
                .success(false)
                .message("Mevcut şifre hatalı")
                .build();

        when(authService.changePassword(anyString(), any())).thenReturn(response);

        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
        request.setCurrentPassword("WrongPass!");
        request.setNewPassword("NewPass123!");
        request.setConfirmPassword("NewPass123!");

        mockMvc.perform(post("/api/v1/me/change-password")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Kullanıcı adı güncelleme başarılı olmalı")
    void updateUsername_Success_ReturnsOk() throws Exception {
        doNothing().when(userService).updateUsername(anyString(), anyString());

        UpdateUsernameRequestDTO request = new UpdateUsernameRequestDTO();
        request.setNewUsername("newusername");

        mockMvc.perform(put("/api/v1/me/username")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Kullanıcı adı güncelleme başarısız olduğunda 400 dönmeli")
    void updateUsername_Failed_ReturnsBadRequest() throws Exception {
        doThrow(new RuntimeException("Bu kullanıcı adı zaten kullanılıyor"))
                .when(userService).updateUsername(anyString(), anyString());

        UpdateUsernameRequestDTO request = new UpdateUsernameRequestDTO();
        request.setNewUsername("existinguser");

        mockMvc.perform(put("/api/v1/me/username")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Son şifre değişim tarihi getirilmeli")
    void getPasswordLastChanged_ReturnsOk() throws Exception {
        when(userService.getPasswordLastChanged(anyString()))
                .thenReturn(LocalDateTime.now().minusDays(5));

        mockMvc.perform(get("/api/v1/me/password-last-changed")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Hesap silme başarıyla tamamlanmalı")
    void deleteAccount_Success_ReturnsOk() throws Exception {
        doNothing().when(userService).deleteAccount(anyString());

        mockMvc.perform(delete("/api/v1/me/account")
                        .with(jwt().jwt(j -> j.subject("test-keycloak-id")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Token olmadan 401 dönmeli")
    void anyEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isUnauthorized());
    }
}
