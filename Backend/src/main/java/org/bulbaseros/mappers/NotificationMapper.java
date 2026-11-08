package org.bulbaseros.mappers;

import org.bulbaseros.dto.NotificationResponse;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Notification;
import org.bulbaseros.entity.Usuario;
import org.springframework.stereotype.Component;

/**
 * Mapeador de componentes Spring para la transformación entre la entidad de persistencia
 * {@link Notification} y el objeto de transferencia de datos {@link NotificationResponse}.
 * <p>
 * Facilita el desacoplamiento entre el modelo relacional JPA y la API REST externa,
 * transformando de forma segura las referencias a incidencias y usuarios a tipos primitivos serializables.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class NotificationMapper extends BaseMapper<Notification, NotificationResponse> {

    /**
     * Transforma una entidad de persistencia {@link Notification} a su correspondiente DTO {@link NotificationResponse}.
     *
     * @param entity Entidad de notificación de origen, o {@code null}.
     * @return DTO {@link NotificationResponse} proyectado, o {@code null} si la entidad es nula.
     */
    @Override
    public NotificationResponse toDto(Notification entity) {
        if (entity == null) {
            return null;
        }

        Long incidentId = (entity.getIncident() != null) ? entity.getIncident().getId() : null;

        return NotificationResponse.builder()
                .id(entity.getId())
                .incidentId(incidentId)
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Construye una nueva entidad {@link Notification} a partir de los datos y entidades de dominio asociadas.
     *
     * @param user     Usuario destinatario de la notificación.
     * @param incident Incidencia urbana asociada al aviso.
     * @param type     Tipo funcional de aviso.
     * @param title    Título del aviso.
     * @param message  Detalle textual del aviso.
     * @return Nueva entidad {@link Notification} lista para persistencia.
     */
    public Notification toEntity(Usuario user, Incidencia incident, String type, String title, String message) {
        return new Notification(user, incident, type, title, message);
    }
}
