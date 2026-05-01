package com.financeportal.backend.Notification;

import com.financeportal.backend.User.Repository.UserRepository;
import com.financeportal.backend.Util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Bildirim oluştur
    public Notification createNotification(String userId, String title, String message,
                                           NotificationType type, String relatedId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .relatedId(relatedId)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("✅ Notification created for user {}: {}", userId, title);
        return saved;
    }

    // Kullanıcının bildirimlerini getir
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(int page, int size) {
        String userId = SecurityUtils.getCurrentUserKeycloakId();
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    // Okunmamış bildirimleri getir
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications() {
        String userId = SecurityUtils.getCurrentUserKeycloakId();
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // Okunmamış bildirim sayısı
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        String userId = SecurityUtils.getCurrentUserKeycloakId();
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    // Tüm bildirimleri okundu işaretle
    @Transactional
    public void markAllAsRead() {
        String userId = SecurityUtils.getCurrentUserKeycloakId();
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("✅ All notifications marked as read for user: {}", userId);
    }

    // Tek bildirimi okundu işaretle
    @Transactional
    public void markAsRead(Long notificationId) {
        String userId = SecurityUtils.getCurrentUserKeycloakId();
        notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
    }

    // Portföy değişimi bildirimi
    public void notifyPortfolioChange(String userId, String portfolioName,
                                      double changePercent, Long portfolioId) {
        // Kullanıcı tercihi kontrol et
        com.financeportal.backend.User.Entity.User user = userRepository.findByKeycloakId(userId).orElse(null);
        if (user == null || !user.isNotifyPortfolioChange()) return;

        String direction = changePercent >= 0 ? "arttı" : "düştü";
        String title = "Portföy Değer Değişimi";
        String message = String.format("%s portföyünüz %%%.2f %s",
                portfolioName, Math.abs(changePercent), direction);

        createNotification(userId, title, message,
                NotificationType.PORTFOLIO_CHANGE, String.valueOf(portfolioId));
    }

    // İşlem bildirimi
    public void notifyTransaction(String userId, String instrumentSymbol,
                                  String transactionType, double quantity, Long portfolioId) {
        // Kullanıcı tercihi kontrol et
        com.financeportal.backend.User.Entity.User user = userRepository.findByKeycloakId(userId).orElse(null);
        if (user == null || !user.isNotifyTransaction()) return;

        String action = transactionType.equals("BUY") ? "alındı" : "satıldı";
        String title = "İşlem Gerçekleşti";
        String message = quantity == Math.floor(quantity)
                ? String.format("%.0f adet %s %s", quantity, instrumentSymbol, action)
                : String.format("%.4f adet %s %s", quantity, instrumentSymbol, action);

        createNotification(userId, title, message,
                NotificationType.TRANSACTION, String.valueOf(portfolioId));
    }

    // Fiyat alarmı bildirimi
    public void notifyPriceAlert(String userId, String instrumentSymbol,
                                 double currentPrice, String currency) {
        // Kullanıcı tercihi kontrol et
        com.financeportal.backend.User.Entity.User user = userRepository.findByKeycloakId(userId).orElse(null);
        if (user == null || !user.isNotifyPriceAlert()) return;

        String title = "Fiyat Alarmı";
        String message = String.format("%s güncel fiyatı: %.2f %s",
                instrumentSymbol, currentPrice, currency);

        createNotification(userId, title, message,
                NotificationType.PRICE_ALERT, instrumentSymbol);
    }

    // Haber bildirimi
    public void notifyNews(String userId, String newsTitle, Long newsId) {
        // Kullanıcı tercihi kontrol et
        com.financeportal.backend.User.Entity.User user = userRepository.findByKeycloakId(userId).orElse(null);
        if (user == null || !user.isNotifyNews()) return;

        String title = "Yeni Haber";
        String message = newsTitle;

        createNotification(userId, title, message,
                NotificationType.NEWS, String.valueOf(newsId));
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .read(n.isRead())
                .relatedId(n.getRelatedId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
