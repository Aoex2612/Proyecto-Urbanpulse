package org.bulbaseros.controller;

import org.bulbaseros.dto.NotificationResponse;
import org.bulbaseros.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST que expone los endpoints para la gestión y consulta de notificaciones in-app.
 * <p>
 * Proporciona acceso seguro a las notificaciones asociadas al usuario autenticado, permitiendo
 * recuperar la lista cronológica descendente de avisos, obtener el conteo de notificaciones
 * pendientes de lectura para alimentar el badge interactivo de la campana y marcar notificaciones
 * individuales como leídas asegurando aislamiento de datos por usuario.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Construye una nueva instancia de {@link NotificationController} inyectando el servicio de notificaciones.
     *
     * @param notificationService Servicio de lógica de negocio transaccional de notificaciones.
     */
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Recupera todas las notificaciones pertenecientes al usuario autenticado en orden cronológico descendente.
     * <p>
     * Endpoint protegido que requiere autenticación previa mediante token JWT en la cabecera {@code Authorization}.
     * </p>
     *
     * @param authentication Contexto de autenticación de Spring Security para identificar al usuario emisor.
     * @return {@link ResponseEntity} con código HTTP 200 OK y la lista de {@link NotificationResponse}.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(Authentication authentication) {
        String email = extractUserEmail(authentication);
        List<NotificationResponse> notifications = notificationService.getUserNotifications(email);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Obtiene el número total de notificaciones no leídas correspondientes al usuario autenticado.
     * <p>
     * Endpoint ligero utilizado para mantener sincronizado el badge numérico de la barra de navegación.
     * </p>
     *
     * @param authentication Contexto de autenticación de Spring Security para identificar al usuario emisor.
     * @return {@link ResponseEntity} con código HTTP 200 OK y un mapa con la clave {@code count} y el valor numérico.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        String email = extractUserEmail(authentication);
        long count = notificationService.getUnreadCount(email);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Marca una notificación específica como leída para el usuario autenticado.
     * <p>
     * Valida de forma estricta que la notificación identificada pertenezca al usuario solicitante,
     * impidiendo accesos no autorizados a avisos de otros usuarios.
     * </p>
     *
     * @param id             Identificador único de la notificación a marcar como leída.
     * @param authentication Contexto de autenticación de Spring Security para identificar al usuario emisor.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link NotificationResponse} actualizado.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable("id") Long id, Authentication authentication) {
        String email = extractUserEmail(authentication);
        NotificationResponse response = notificationService.markAsRead(id, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Extrae de forma segura el correo electrónico principal a partir del contexto de autenticación de Spring Security.
     *
     * @param authentication Contexto de autenticación del usuario solicitante.
     * @return Dirección de correo electrónico del usuario autenticado, o {@code null} si la autenticación es nula.
     */
    private String extractUserEmail(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
