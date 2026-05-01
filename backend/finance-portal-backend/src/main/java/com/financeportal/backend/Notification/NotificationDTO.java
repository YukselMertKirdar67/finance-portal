package com.financeportal.backend.Notification;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private String relatedId;
    private LocalDateTime createdAt;
}