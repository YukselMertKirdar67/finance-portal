package com.financeportal.backend.User;

import com.financeportal.backend.User.Controller.AdminController;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Service.AdminService;
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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@ActiveProfiles("test")
@DisplayName("AdminController Testleri")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    private UserResponseDTO testUserDTO;
    private AdminStatsDTO testStatsDTO;

    @BeforeEach
    void setUp() {
        testUserDTO = UserResponseDTO.builder()
                .id("test-id-123")
                .username("testuser")
                .email("test@test.com")
                .enabled(true)
                .emailVerified(true)
                .build();

        testStatsDTO = AdminStatsDTO.builder()
                .totalUsers(10L)
                .activeUsers(8L)
                .disabledUsers(2L)
                .totalPortfolios(15L)
                .activePortfolios(12L)
                .totalPortfolioValue(BigDecimal.valueOf(100000))
                .totalTransactions(50L)
                .buyTransactions(30L)
                .sellTransactions(20L)
                .totalWatchlistItems(25L)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Tüm kullanıcılar getirilmeli")
    void getAllUsers_ReturnsOk() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of(testUserDTO));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin istatistikleri getirilmeli")
    void getAdminStats_ReturnsOk() throws Exception {
        when(adminService.getAdminStats()).thenReturn(testStatsDTO);

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.activeUsers").value(8));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Kullanıcı devre dışı bırakılmalı")
    void disableUser_ReturnsOk() throws Exception {
        doNothing().when(adminService).disableUser("test-id-123");

        mockMvc.perform(put("/api/v1/admin/users/test-id-123/disable")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin ping başarılı olmalı")
    void ping_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ping"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getAllUsers_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}