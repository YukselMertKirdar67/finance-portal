package com.financeportal.backend.Notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
@DisplayName("NotificationController Testleri")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private NotificationDTO testNotificationDTO;

    @BeforeEach
    void setUp() {
        testNotificationDTO = NotificationDTO.builder()
                .id(1L)
                .title("Test Bildirim")
                .message("Test mesajı")
                .type(NotificationType.PRICE_ALERT)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Bildirimler sayfalı olarak getirilmeli")
    void getNotifications_ReturnsOk() throws Exception {
        Page<NotificationDTO> page = new PageImpl<>(List.of(testNotificationDTO));
        when(notificationService.getNotifications(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Bildirim"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Okunmamış bildirimler getirilmeli")
    void getUnreadNotifications_ReturnsOk() throws Exception {
        when(notificationService.getUnreadNotifications()).thenReturn(List.of(testNotificationDTO));

        mockMvc.perform(get("/api/v1/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Bildirim"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Okunmamış bildirim sayısı getirilmeli")
    void getUnreadCount_ReturnsOk() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Tüm bildirimler okundu işaretlenmeli")
    void markAllAsRead_ReturnsOk() throws Exception {
        doNothing().when(notificationService).markAllAsRead();

        mockMvc.perform(put("/api/v1/notifications/mark-all-read")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Bildirim okundu işaretlenmeli")
    void markAsRead_ReturnsOk() throws Exception {
        doNothing().when(notificationService).markAsRead(1L);

        mockMvc.perform(put("/api/v1/notifications/1/read")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Token olmadan erişim - 401")
    void getNotifications_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }
}