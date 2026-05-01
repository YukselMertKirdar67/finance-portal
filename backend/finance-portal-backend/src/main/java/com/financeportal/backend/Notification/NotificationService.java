package com.financeportal.backend.Notification;

import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationService {

    Notification createNotification(String userId, String title, String message,
                                    NotificationType type, String relatedId);

    Page<NotificationDTO> getNotifications(int page, int size);

    List<NotificationDTO> getUnreadNotifications();

    long getUnreadCount();

    void markAllAsRead();

    void markAsRead(Long notificationId);

    void notifyPortfolioChange(String userId, String portfolioName,
                               double changePercent, Long portfolioId);

    void notifyTransaction(String userId, String instrumentSymbol,
                           String transactionType, double quantity, Long portfolioId);

    void notifyPriceAlert(String userId, String instrumentSymbol,
                          double currentPrice, String currency);

    void notifyNews(String userId, String newsTitle, Long newsId);
}