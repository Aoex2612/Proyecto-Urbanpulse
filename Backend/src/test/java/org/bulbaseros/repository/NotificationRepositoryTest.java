package org.bulbaseros.repository;

import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Notification;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración y persistencia Spring Data JPA para {@link NotificationRepository}.
 * <p>
 * Valida mediante {@link DataJpaTest} y {@link TestEntityManager} la correcta persistencia
 * de la entidad {@link Notification}, el ciclo de vida {@code @PrePersist}, el orden cronológico
 * descendente en listados, el conteo rápido de no leídas y el aislamiento seguro por usuario.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    private Usuario citizen;
    private Usuario operator;
    private Incidencia incident;

    @BeforeEach
    void setUp() {
        citizen = new Usuario("ciudadano.notif@urbanpulse.es", "$2a$10$xyzHashedPass", "Ciudadano Notif", RolUsuario.CITIZEN);
        citizen = entityManager.persistAndFlush(citizen);

        operator = new Usuario("operador.notif@urbanpulse.es", "$2a$10$xyzHashedPass", "Operador Notif", RolUsuario.OPERATOR);
        operator = entityManager.persistAndFlush(operator);

        incident = new Incidencia("Bache en calzada", "Bache profundo en calle Larios", "Vías públicas", 36.72, -4.42, citizen);
        incident = entityManager.persistAndFlush(incident);
    }

    @Test
    @DisplayName("Debe persistir una notificación con valores predeterminados y ciclo @PrePersist")
    void shouldPersistNotificationWithPrePersistDefaults() {
        Notification notification = new Notification();
        notification.setUser(citizen);
        notification.setIncident(incident);
        notification.setType("STATUS_CHANGE");
        notification.setTitle("Incidencia Validada");
        notification.setMessage("Su incidencia ha sido admitida a trámite.");

        Notification persisted = notificationRepository.saveAndFlush(notification);

        assertNotNull(persisted.getId(), "El ID debe ser autogenerado");
        assertNotNull(persisted.getCreatedAt(), "createdAt debe ser asignado automáticamente en @PrePersist");
        assertFalse(persisted.getIsRead(), "isRead debe inicializarse en false por defecto");
        assertEquals("STATUS_CHANGE", persisted.getType());
        assertEquals("Incidencia Validada", persisted.getTitle());
    }

    @Test
    @DisplayName("Debe listar notificaciones de un usuario en orden cronológico descendente")
    void shouldFindNotificationsByUserIdOrderByCreatedAtDesc() {
        OffsetDateTime now = OffsetDateTime.now();

        Notification n1 = Notification.builder()
                .user(citizen)
                .incident(incident)
                .type("STATUS_CHANGE")
                .title("Aviso 1")
                .message("Primer aviso más antiguo")
                .isRead(false)
                .createdAt(now.minusHours(3))
                .build();
        entityManager.persistAndFlush(n1);

        Notification n2 = Notification.builder()
                .user(citizen)
                .incident(incident)
                .type("STATUS_CHANGE")
                .title("Aviso 2")
                .message("Segundo aviso intermedio")
                .isRead(true)
                .createdAt(now.minusHours(1))
                .build();
        entityManager.persistAndFlush(n2);

        Notification n3 = Notification.builder()
                .user(citizen)
                .incident(incident)
                .type("DUPLICATE_SUGGESTION")
                .title("Aviso 3")
                .message("Tercer aviso más reciente")
                .isRead(false)
                .createdAt(now)
                .build();
        entityManager.persistAndFlush(n3);

        // Notificación de otro usuario (operador) que no debe figurar en la lista del ciudadano
        Notification nOther = Notification.builder()
                .user(operator)
                .incident(incident)
                .type("DUPLICATE_SUGGESTION")
                .title("Aviso operador")
                .message("Alerta de duplicado para operador")
                .isRead(false)
                .createdAt(now)
                .build();
        entityManager.persistAndFlush(nOther);

        List<Notification> citizenNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(citizen.getId());

        assertEquals(3, citizenNotifications.size(), "Debe recuperar exactamente las 3 notificaciones del ciudadano");
        assertEquals("Aviso 3", citizenNotifications.get(0).getTitle(), "La más reciente debe aparecer en primer lugar");
        assertEquals("Aviso 2", citizenNotifications.get(1).getTitle());
        assertEquals("Aviso 1", citizenNotifications.get(2).getTitle(), "La más antigua debe figurar al final");
        assertTrue(citizenNotifications.get(0).getCreatedAt().isAfter(citizenNotifications.get(1).getCreatedAt()));
        assertTrue(citizenNotifications.get(1).getCreatedAt().isAfter(citizenNotifications.get(2).getCreatedAt()));
    }

    @Test
    @DisplayName("Debe contabilizar correctamente las notificaciones pendientes de lectura del usuario")
    void shouldCountUnreadNotificationsByUserId() {
        OffsetDateTime now = OffsetDateTime.now();

        // 2 no leídas para citizen
        Notification unread1 = Notification.builder()
                .user(citizen)
                .incident(incident)
                .type("STATUS_CHANGE")
                .title("Aviso 1")
                .message("Mensaje 1")
                .isRead(false)
                .createdAt(now.minusMinutes(10))
                .build();
        entityManager.persistAndFlush(unread1);

        Notification unread2 = Notification.builder()
                .user(citizen)
                .incident(incident)
                .type("STATUS_CHANGE")
                .title("Aviso 2")
                .message("Mensaje 2")
                .isRead(false)
                .createdAt(now.minusMinutes(5))
                .build();
        entityManager.persistAndFlush(unread2);

        // 1 leída para citizen
        Notification readCitizen = Notification.builder()
                .user(citizen)
                .incident(incident)
                .type("STATUS_CHANGE")
                .title("Aviso Leído")
                .message("Mensaje ya visto")
                .isRead(true)
                .createdAt(now.minusHours(1))
                .build();
        entityManager.persistAndFlush(readCitizen);

        // 1 no leída para operator (no debe sumar al conteo de citizen)
        Notification unreadOperator = Notification.builder()
                .user(operator)
                .incident(incident)
                .type("DUPLICATE_SUGGESTION")
                .title("Aviso Operador")
                .message("Mensaje operador")
                .isRead(false)
                .createdAt(now)
                .build();
        entityManager.persistAndFlush(unreadOperator);

        long citizenUnreadCount = notificationRepository.countByUserIdAndIsReadFalse(citizen.getId());
        long operatorUnreadCount = notificationRepository.countByUserIdAndIsReadFalse(operator.getId());

        assertEquals(2L, citizenUnreadCount, "El ciudadano debe tener exactamente 2 notificaciones no leídas");
        assertEquals(1L, operatorUnreadCount, "El operador debe tener exactamente 1 notificación no leída");
    }

    @Test
    @DisplayName("Debe encontrar notificación por id y userId, y aislar accesos cruzados entre usuarios")
    void shouldFindByIdAndUserIdWithUserIsolation() {
        Notification notification = Notification.builder()
                .user(citizen)
                .incident(incident)
                .type("STATUS_CHANGE")
                .title("Aviso Privado")
                .message("Notificación confidencial")
                .isRead(false)
                .createdAt(OffsetDateTime.now())
                .build();
        Notification saved = entityManager.persistAndFlush(notification);

        // Búsqueda legítima por titular
        Optional<Notification> found = notificationRepository.findByIdAndUserId(saved.getId(), citizen.getId());
        assertTrue(found.isPresent(), "Debe encontrar la notificación si el ID y userId coinciden");
        assertEquals("Aviso Privado", found.get().getTitle());

        // Intento de acceso por otro usuario (IDOR prevention)
        Optional<Notification> unauthorizedAccess = notificationRepository.findByIdAndUserId(saved.getId(), operator.getId());
        assertTrue(unauthorizedAccess.isEmpty(), "No debe retornar la notificación si el userId no coincide con el destinatario");

        // Búsqueda con ID inexistente
        Optional<Notification> notFound = notificationRepository.findByIdAndUserId(999999L, citizen.getId());
        assertTrue(notFound.isEmpty(), "Debe retornar vacío si el ID no existe");
    }
}
