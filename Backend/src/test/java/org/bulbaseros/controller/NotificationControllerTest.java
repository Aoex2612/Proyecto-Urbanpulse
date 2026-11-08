package org.bulbaseros.controller;

import org.bulbaseros.config.SecurityConfig;
import org.bulbaseros.dto.NotificationResponse;
import org.bulbaseros.exception.GlobalExceptionHandler;
import org.bulbaseros.security.JwtService;
import org.bulbaseros.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración web y seguridad RBAC con MockMvc para el controlador {@link NotificationController}.
 * <p>
 * Verifica los contratos HTTP de la API REST de notificaciones in-app:
 * <ul>
 *   <li>Autenticación obligatoria (401 Unauthorized) ante peticiones anónimas en todos los endpoints.</li>
 *   <li>Recuperación exitosa (200 OK) del listado cronológico de notificaciones del usuario autenticado.</li>
 *   <li>Consulta rápida (200 OK) del conteo de avisos pendientes de lectura ({@code /unread-count}).</li>
 *   <li>Marcado individual como leída (200 OK) vía {@code PATCH /api/notifications/{id}/read}.</li>
 *   <li>Respuesta 404 Not Found cuando la notificación no existe o no pertenece al usuario emisor.</li>
 * </ul>
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtService jwtService;

    private static final String TEST_EMAIL = "ciudadano@urbanpulse.es";

    @Test
    @DisplayName("GET /api/notifications sin autenticación debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnGetNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count sin autenticación debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnGetUnreadCount() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read sin autenticación debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnMarkAsRead() throws Exception {
        mockMvc.perform(patch("/api/notifications/10/read"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "CITIZEN")
    @DisplayName("GET /api/notifications con usuario autenticado retorna 200 OK y la lista de avisos")
    void shouldReturnNotificationsListWhenAuthenticated() throws Exception {
        NotificationResponse notif1 = NotificationResponse.builder()
                .id(1L)
                .incidentId(101L)
                .type("STATUS_CHANGE")
                .title("Incidencia validada")
                .message("Tu reporte ha sido validado correctamente")
                .isRead(false)
                .createdAt(OffsetDateTime.now())
                .build();

        NotificationResponse notif2 = NotificationResponse.builder()
                .id(2L)
                .incidentId(102L)
                .type("DUPLICATE_SUGGESTION")
                .title("Posible duplicado detectado")
                .message("Se ha detectado una incidencia similar cercana")
                .isRead(true)
                .createdAt(OffsetDateTime.now().minusHours(1))
                .build();

        when(notificationService.getUserNotifications(TEST_EMAIL)).thenReturn(List.of(notif1, notif2));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].incidentId").value(101))
                .andExpect(jsonPath("$[0].type").value("STATUS_CHANGE"))
                .andExpect(jsonPath("$[0].title").value("Incidencia validada"))
                .andExpect(jsonPath("$[0].message").value("Tu reporte ha sido validado correctamente"))
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].isRead").value(true));

        verify(notificationService).getUserNotifications(TEST_EMAIL);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "CITIZEN")
    @DisplayName("GET /api/notifications/unread-count con usuario autenticado retorna 200 OK y el conteo esperado")
    void shouldReturnUnreadCountWhenAuthenticated() throws Exception {
        when(notificationService.getUnreadCount(TEST_EMAIL)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));

        verify(notificationService).getUnreadCount(TEST_EMAIL);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "CITIZEN")
    @DisplayName("PATCH /api/notifications/{id}/read con usuario autenticado retorna 200 OK y la notificación marcada")
    void shouldMarkNotificationAsReadWhenAuthenticated() throws Exception {
        NotificationResponse updated = NotificationResponse.builder()
                .id(15L)
                .incidentId(205L)
                .type("STATUS_CHANGE")
                .title("Incidencia en progreso")
                .message("Un técnico ha comenzado la intervención")
                .isRead(true)
                .createdAt(OffsetDateTime.now())
                .build();

        when(notificationService.markAsRead(15L, TEST_EMAIL)).thenReturn(updated);

        mockMvc.perform(patch("/api/notifications/15/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.incidentId").value(205))
                .andExpect(jsonPath("$.type").value("STATUS_CHANGE"))
                .andExpect(jsonPath("$.title").value("Incidencia en progreso"))
                .andExpect(jsonPath("$.isRead").value(true));

        verify(notificationService).markAsRead(15L, TEST_EMAIL);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "CITIZEN")
    @DisplayName("PATCH /api/notifications/{id}/read retorna 404 Not Found si la notificación no pertenece al usuario")
    void shouldReturn404WhenNotificationNotFoundOrNotOwned() throws Exception {
        when(notificationService.markAsRead(eq(999L), eq(TEST_EMAIL)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada o no pertenece al usuario"));

        mockMvc.perform(patch("/api/notifications/999/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Notificación no encontrada o no pertenece al usuario"));

        verify(notificationService).markAsRead(999L, TEST_EMAIL);
    }
}
