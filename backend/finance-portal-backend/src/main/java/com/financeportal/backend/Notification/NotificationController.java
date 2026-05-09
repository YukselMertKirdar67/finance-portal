package com.financeportal.backend.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Log4j2
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Kullanıcının bildirimlerini sayfalı olarak getirir.
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching notifications page: {}, size: {}", page, size);
        return ResponseEntity.ok(notificationService.getNotifications(page, size));
    }

    /**
     * Kullanıcının okunmamış bildirimlerini getirir.
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {
        log.info("Fetching unread notifications");
        return ResponseEntity.ok(notificationService.getUnreadNotifications());
    }

    /**
     * Kullanıcının okunmamış bildirim sayısını getirir.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        log.info("Fetching unread notification count");
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount()));
    }

    /**
     * Tüm bildirimleri okundu olarak işaretler.
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead() {
        log.info("Marking all notifications as read");
        notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Belirtilen bildirimi okundu olarak işaretler.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        log.info("Marking notification {} as read", id);
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}