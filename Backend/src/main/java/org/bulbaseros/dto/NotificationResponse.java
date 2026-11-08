package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) de respuesta que expone la información desacoplada de una notificación.
 * <p>
 * Diseñado para la serialización y consumo en clientes API y frontend, proyectando la relación
 * de la incidencia como un identificador simple ({@code incidentId}) sin exponer entidades JPA de dominio.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    /**
     * Identificador único de la notificación.
     */
    private Long id;

    /**
     * Identificador de la incidencia urbana asociada al aviso.
     */
    private Long incidentId;

    /**
     * Tipo funcional del evento notificado (ej. STATUS_CHANGE, DUPLICATE_SUGGESTION).
     */
    private String type;

    /**
     * Título informativo del aviso.
     */
    private String title;

    /**
     * Contenido textual explicativo del aviso.
     */
    private String message;

    /**
     * Indicador de si el aviso ha sido leído por el usuario.
     */
    private Boolean isRead;

    /**
     * Fecha y hora en la que fue generada la notificación.
     */
    private OffsetDateTime createdAt;
}
