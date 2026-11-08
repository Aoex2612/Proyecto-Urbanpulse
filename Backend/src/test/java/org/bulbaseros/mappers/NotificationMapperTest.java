package org.bulbaseros.mappers;

import org.bulbaseros.dto.NotificationResponse;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Notification;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link NotificationMapper}.
 * <p>
 * Verifica la correcta transformación bidireccional entre la entidad {@link Notification}
 * y el DTO desacoplado {@link NotificationResponse}, cubriendo casos con entidad completa,
 * referencias nulas, colecciones y métodos constructores delegados.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapper();

    @Test
    @DisplayName("Debe retornar null cuando la entidad Notification suministrada es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    @DisplayName("Debe transformar correctamente todos los atributos de Notification a NotificationResponse")
    void shouldMapEntityToDtoCorrectly() {
        Incidencia incident = new Incidencia();
        incident.setId(42L);

        Usuario user = new Usuario();
        user.setId(7L);

        OffsetDateTime now = OffsetDateTime.now();

        Notification notification = Notification.builder()
                .id(101L)
                .user(user)
                .incident(incident)
                .type("STATUS_CHANGE")
                .title("Estado actualizado")
                .message("La incidencia ha pasado a EN PROCESO")
                .isRead(true)
                .createdAt(now)
                .build();

        NotificationResponse response = mapper.toDto(notification);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(42L, response.getIncidentId());
        assertEquals("STATUS_CHANGE", response.getType());
        assertEquals("Estado actualizado", response.getTitle());
        assertEquals("La incidencia ha pasado a EN PROCESO", response.getMessage());
        assertTrue(response.getIsRead());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    @DisplayName("Debe mapear de forma segura una notificación con relación a incidencia nula")
    void shouldMapEntityWithNullIncidentSafely() {
        Notification notification = Notification.builder()
                .id(102L)
                .incident(null)
                .type("DUPLICATE_SUGGESTION")
                .title("Posible duplicado")
                .message("Se detectó una incidencia similar")
                .isRead(false)
                .build();

        NotificationResponse response = mapper.toDto(notification);

        assertNotNull(response);
        assertEquals(102L, response.getId());
        assertNull(response.getIncidentId());
        assertEquals("DUPLICATE_SUGGESTION", response.getType());
        assertEquals("Posible duplicado", response.getTitle());
        assertEquals("Se detectó una incidencia similar", response.getMessage());
        assertFalse(response.getIsRead());
        assertNull(response.getCreatedAt());
    }

    @Test
    @DisplayName("Debe transformar una lista de entidades Notification a lista de DTOs")
    void shouldMapEntityListToDtoList() {
        Incidencia incident = new Incidencia();
        incident.setId(5L);

        Notification n1 = Notification.builder()
                .id(1L)
                .incident(incident)
                .type("STATUS_CHANGE")
                .title("Aviso 1")
                .message("Mensaje 1")
                .isRead(false)
                .build();

        Notification n2 = Notification.builder()
                .id(2L)
                .incident(incident)
                .type("DUPLICATE_SUGGESTION")
                .title("Aviso 2")
                .message("Mensaje 2")
                .isRead(true)
                .build();

        List<NotificationResponse> dtos = mapper.toDtoList(List.of(n1, n2));

        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals(5L, dtos.get(0).getIncidentId());
        assertEquals(2L, dtos.get(1).getId());
    }

    @Test
    @DisplayName("Debe manejar listas nulas o vacías retornando lista vacía inmutable")
    void shouldHandleNullOrEmptyListSafely() {
        assertTrue(mapper.toDtoList(null).isEmpty());
        assertTrue(mapper.toDtoList(Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("Debe construir una entidad Notification a través del método toEntity")
    void shouldConstructEntityViaToEntityMethod() {
        Usuario user = new Usuario();
        user.setId(10L);

        Incidencia incident = new Incidencia();
        incident.setId(20L);

        Notification entity = mapper.toEntity(user, incident, "STATUS_CHANGE", "Título Test", "Mensaje Test");

        assertNotNull(entity);
        assertEquals(user, entity.getUser());
        assertEquals(incident, entity.getIncident());
        assertEquals("STATUS_CHANGE", entity.getType());
        assertEquals("Título Test", entity.getTitle());
        assertEquals("Mensaje Test", entity.getMessage());
        assertFalse(entity.getIsRead());
        assertNotNull(entity.getCreatedAt());
    }
}
