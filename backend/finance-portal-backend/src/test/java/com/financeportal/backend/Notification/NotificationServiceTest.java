package com.financeportal.backend.Notification;

import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Repository.UserRepository;
import com.financeportal.backend.Util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final String TEST_USER_ID = "test-keycloak-id-123";

    private Notification testNotification;
    private User testUser;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .userId(TEST_USER_ID)
                .title("Test Bildirim")
                .message("Test mesajı")
                .type(NotificationType.PRICE_ALERT)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        testUser = new User();
        testUser.setKeycloakId(TEST_USER_ID);
        testUser.setNotifyPriceAlert(true);
        testUser.setNotifyPortfolioChange(true);
        testUser.setNotifyTransaction(true);
        testUser.setNotifyNews(true);
    }

    @Test
    @DisplayName("Bildirim başarıyla oluşturulmalı")
    void createNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.createNotification(
                TEST_USER_ID, "Test Bildirim", "Test mesajı",
                NotificationType.PRICE_ALERT, "USD/TRY");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Bildirim");
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Kullanıcının bildirimleri sayfalı getirilmeli")
    void getNotifications_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            Page<Notification> page = new PageImpl<>(List.of(testNotification));
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    eq(TEST_USER_ID), any(PageRequest.class))).thenReturn(page);

            Page<NotificationDTO> result = notificationService.getNotifications(0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Bildirim");
        }
    }

    @Test
    @DisplayName("Okunmamış bildirim sayısı doğru dönmeli")
    void getUnreadCount_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(notificationRepository.countByUserIdAndReadFalse(TEST_USER_ID)).thenReturn(3L);

            long count = notificationService.getUnreadCount();

            assertThat(count).isEqualTo(3L);
        }
    }

    @Test
    @DisplayName("Fiyat alarmı bildirimi kullanıcı tercihi açıksa gönderilmeli")
    void notifyPriceAlert_UserPrefEnabled_Success() {
        when(userRepository.findByKeycloakId(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyPriceAlert(TEST_USER_ID, "USD/TRY", 40.00, "TRY");

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Fiyat alarmı bildirimi kullanıcı tercihi kapalıysa gönderilmemeli")
    void notifyPriceAlert_UserPrefDisabled_NotSent() {
        testUser.setNotifyPriceAlert(false);
        when(userRepository.findByKeycloakId(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        notificationService.notifyPriceAlert(TEST_USER_ID, "USD/TRY", 40.00, "TRY");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kullanıcı bulunamazsa bildirim gönderilmemeli")
    void notifyPriceAlert_UserNotFound_NotSent() {
        when(userRepository.findByKeycloakId(TEST_USER_ID)).thenReturn(Optional.empty());

        notificationService.notifyPriceAlert(TEST_USER_ID, "USD/TRY", 40.00, "TRY");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tüm bildirimler okundu işaretlenmeli")
    void markAllAsRead_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            notificationService.markAllAsRead();

            verify(notificationRepository, times(1)).markAllAsReadByUserId(TEST_USER_ID);
        }
    }

    @Test
    @DisplayName("Bildirim okundu olarak işaretlenmeli")
    void markAsRead_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            notificationService.markAsRead(1L);

            verify(notificationRepository, times(1)).markAsReadByIdAndUserId(1L, TEST_USER_ID);
        }
    }
}