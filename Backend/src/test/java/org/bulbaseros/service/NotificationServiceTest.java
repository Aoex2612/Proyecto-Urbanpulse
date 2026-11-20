package org.bulbaseros.service;

import org.bulbaseros.dto.NotificationResponse;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Notification;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.mappers.NotificationMapper;
import org.bulbaseros.repository.NotificationRepository;
import org.bulbaseros.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias con Mockito para el servicio de notificaciones {@link NotificationService}.
 * <p>
 * Valida la lógica de negocio, persistencia atómica, consulta cronológica descendente,
 * conteo reactivo de pendientes, marcado de lectura y control estricto de titularidad/aislamiento.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private Usuario testUser;
    private Incidencia testIncident;
    private Notification testNotification;
    private NotificationResponse testResponseDto;

    @BeforeEach
    void setUp() {
        testUser = new Usuario("ciudadano@urbanpulse.es", "hash123", "Juan Pérez", RolUsuario.CITIZEN);
        testUser.setId(1L);

        testIncident = new Incidencia("Farola rota en calle Larios", "No funciona el alumbrado público", "ALUMBRADO", 36.72, -4.42, testUser);
        testIncident.setId(100L);

        testNotification = Notification.builder()
                .id(10L)
                .user(testUser)
                .incident(testIncident)
                .type("STATUS_CHANGE")
                .title("Incidencia validada")
                .message("Tu incidencia ha sido validada por el operador.")
                .isRead(false)
                .createdAt(OffsetDateTime.now())
                .build();

        testResponseDto = NotificationResponse.builder()
                .id(10L)
                .incidentId(100L)
                .type("STATUS_CHANGE")
                .title("Incidencia validada")
                .message("Tu incidencia ha sido validada por el operador.")
                .isRead(false)
                .createdAt(testNotification.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("createNotification debe instanciar y persistir una nueva notificación")
    void shouldCreateAndPersistNotification() {
        when(notificationMapper.toEntity(testUser, testIncident, "STATUS_CHANGE", "Incidencia validada", "Mensaje"))
                .thenReturn(testNotification);
        when(notificationRepository.save(testNotification)).thenReturn(testNotification);

        Notification result = notificationService.createNotification(testUser, testIncident, "STATUS_CHANGE", "Incidencia validada", "Mensaje");

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("STATUS_CHANGE", result.getType());
        verify(notificationMapper).toEntity(testUser, testIncident, "STATUS_CHANGE", "Incidencia validada", "Mensaje");
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("getUserNotifications retorna lista de notificaciones en orden cronológico descendente")
    void shouldGetUserNotificationsSuccessfully() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of(testNotification));
        when(notificationMapper.toDtoList(List.of(testNotification))).thenReturn(List.of(testResponseDto));

        List<NotificationResponse> result = notificationService.getUserNotifications(testUser.getEmail());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("Incidencia validada", result.get(0).getTitle());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(testUser.getId());
        verify(notificationMapper).toDtoList(List.of(testNotification));
    }

    @Test
    @DisplayName("getUserNotifications lanza 404 Not Found si el usuario no existe")
    void shouldThrowNotFoundWhenUserDoesNotExistOnGetUserNotifications() {
        when(userRepository.findByEmail("noexiste@urbanpulse.es")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.getUserNotifications("noexiste@urbanpulse.es"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Usuario no encontrado"));
        verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("getUnreadCount retorna el conteo de avisos pendientes del usuario")
    void shouldGetUnreadCountSuccessfully() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(notificationRepository.countByUserIdAndIsReadFalse(testUser.getId())).thenReturn(4L);

        long count = notificationService.getUnreadCount(testUser.getEmail());

        assertEquals(4L, count);
        verify(notificationRepository).countByUserIdAndIsReadFalse(testUser.getId());
    }

    @Test
    @DisplayName("getUnreadCount lanza 404 Not Found si el usuario no existe")
    void shouldThrowNotFoundWhenUserDoesNotExistOnGetUnreadCount() {
        when(userRepository.findByEmail("noexiste@urbanpulse.es")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.getUnreadCount("noexiste@urbanpulse.es"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Usuario no encontrado"));
        verify(notificationRepository, never()).countByUserIdAndIsReadFalse(any());
    }

    @Test
    @DisplayName("markAsRead actualiza el estado de lectura a true y retorna DTO actualizado")
    void shouldMarkNotificationAsReadSuccessfully() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByIdAndUserId(10L, testUser.getId())).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse updatedDto = NotificationResponse.builder()
                .id(10L)
                .incidentId(100L)
                .type("STATUS_CHANGE")
                .title("Incidencia validada")
                .message("Tu incidencia ha sido validada por el operador.")
                .isRead(true)
                .createdAt(testNotification.getCreatedAt())
                .build();

        when(notificationMapper.toDto(any(Notification.class))).thenReturn(updatedDto);

        NotificationResponse result = notificationService.markAsRead(10L, testUser.getEmail());

        assertNotNull(result);
        assertTrue(result.getIsRead());
        assertTrue(testNotification.getIsRead());
        verify(notificationRepository).findByIdAndUserId(10L, testUser.getId());
        verify(notificationRepository).save(testNotification);
        verify(notificationMapper).toDto(testNotification);
    }

    @Test
    @DisplayName("markAsRead lanza 404 Not Found cuando la notificación no existe o no pertenece al usuario")
    void shouldThrowNotFoundWhenNotificationNotOwnedOrNotFound() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByIdAndUserId(999L, testUser.getId())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.markAsRead(999L, testUser.getEmail()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Notificación no encontrada o no pertenece al usuario"));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsRead lanza 404 Not Found si el usuario solicitante no existe")
    void shouldThrowNotFoundWhenUserDoesNotExistOnMarkAsRead() {
        when(userRepository.findByEmail("noexiste@urbanpulse.es")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.markAsRead(10L, "noexiste@urbanpulse.es"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Usuario no encontrado"));
        verify(notificationRepository, never()).findByIdAndUserId(any(), any());
    }
}
