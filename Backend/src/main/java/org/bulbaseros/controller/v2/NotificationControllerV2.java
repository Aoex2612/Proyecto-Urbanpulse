package org.bulbaseros.controller.v2;

import org.bulbaseros.dto.v2.NotificationResponseV2;
import org.bulbaseros.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para la gestion de notificaciones en la API v2.
 * Emplea identificadores publicos universales (UUID v4) en lugar de IDs de BD.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@RestController
@RequestMapping("/api/v2/notifications")
public class NotificationControllerV2 {

    private final NotificationService notificationService;

    @Autowired
    public NotificationControllerV2(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseV2>> getUserNotifications(Authentication authentication) {
        String email = extractUserEmail(authentication);
        return ResponseEntity.ok(notificationService.getUserNotificationsV2(email));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        String email = extractUserEmail(authentication);
        long count = notificationService.getUnreadCount(email);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseV2> markAsRead(@PathVariable("id") UUID id, Authentication authentication) {
        String email = extractUserEmail(authentication);
        return ResponseEntity.ok(notificationService.markAsReadV2(id, email));
    }

    private String extractUserEmail(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
